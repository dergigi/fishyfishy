package org.dergigi.fishyfishy

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/** Immutable revisions avoid two offline devices writing the same file. Null is a deletion. */
data class SwimRevision(val id: String, val tripId: String, val parents: Set<String>, val trip: Trip?)
data class JournalSnapshot(val revisions: List<SwimRevision>) {
    private val groups = revisions.groupBy { it.tripId }
    fun heads(id: String): List<SwimRevision> {
        val entries = groups[id].orEmpty()
        val replaced = entries.flatMap { it.parents }.toSet()
        return entries.filterNot { it.id in replaced }.sortedBy { it.id }
    }
    val conflicts: Map<String, List<SwimRevision>> = groups.keys.mapNotNull { id ->
        heads(id).takeIf { h -> h.map { it.trip }.distinct().size > 1 }?.let { id to it }
    }.toMap()
    // Keep a living version visible during a conflict, but require explicit resolution to edit it.
    val trips: List<Trip> = groups.keys.mapNotNull { id -> heads(id).firstNotNullOfOrNull { it.trip } }
        .sortedWith(compareByDescending<Trip> { it.date }.thenBy { it.id })
}

object RevisionCodec {
    private val uuid = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    private fun digest(raw: String) = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    fun encode(revision: SwimRevision): String {
        val payload = JSONObject().put("id", revision.id).put("tripId", revision.tripId)
            .put("parents", JSONArray(revision.parents.sorted()))
            .put("trip", revision.trip?.let { JournalCodec.encode(listOf(it)) } ?: JSONObject.NULL).toString()
        return JSONObject().put("format", "fishyfishy-revision").put("version", 1)
            .put("payload", payload).put("sha256", digest(payload)).toString(2)
    }
    fun decode(raw: String): SwimRevision {
        require(raw.length <= 262144) { "Revision is too large." }
        val envelope = JSONObject(raw)
        require(envelope.getString("format") == "fishyfishy-revision" && envelope.getInt("version") == 1) { "Unsupported folder format. Update FishyFishy." }
        val rawPayload = envelope.getString("payload")
        require(envelope.getString("sha256") == digest(rawPayload)) { "A swim file is incomplete or damaged." }
        val p = JSONObject(rawPayload)
        val parents = p.getJSONArray("parents")
        val trip = if (p.isNull("trip")) null else JournalCodec.decode(p.getString("trip")).single()
        val revision = SwimRevision(p.getString("id"), p.getString("tripId"), (0 until parents.length()).map { parents.getString(it) }.toSet(), trip)
        require(uuid.matches(revision.id) && revision.parents.all { uuid.matches(it) } && revision.id !in revision.parents)
        require(revision.tripId.isNotBlank() && revision.tripId.length <= 100 && (trip == null || trip.id == revision.tripId))
        return revision
    }
    fun snapshot(revisions: List<SwimRevision>): JournalSnapshot {
        val byId = revisions.groupBy { it.id }
        require(byId.values.all { it.distinct().size == 1 }) { "Conflicting files have the same revision ID." }
        val unique = byId.mapValues { it.value.first() }
        val children = mutableMapOf<String, MutableList<String>>()
        val counts = mutableMapOf<String, Int>()
        unique.values.forEach { r ->
            val known = r.parents.mapNotNull { unique[it] }
            require(known.all { it.tripId == r.tripId }) { "A revision references another swim." }
            counts[r.id] = known.size
            known.forEach { children.getOrPut(it.id) { mutableListOf() }.add(r.id) }
        }
        val queue = java.util.ArrayDeque(counts.filterValues { it == 0 }.keys)
        var visited = 0
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst(); visited++
            children[id].orEmpty().forEach { child ->
                counts[child] = counts.getValue(child) - 1
                if (counts[child] == 0) queue.add(child)
            }
        }
        require(visited == unique.size) { "Swim history contains a cycle." }
        return JournalSnapshot(unique.values.toList())
    }
    fun seed(trip: Trip): SwimRevision {
        val id = UUID.nameUUIDFromBytes(("fishyfishy-migration:" + JournalCodec.encode(listOf(trip))).toByteArray()).toString()
        return SwimRevision(id, trip.id, emptySet(), trip)
    }
}

interface RevisionStore {
    fun read(): List<SwimRevision>
    /** Never overwrite a committed revision, including during migration or a retry. */
    fun append(revision: SwimRevision)
}

class SyncJournal(private val store: RevisionStore) {
    fun read() = RevisionCodec.snapshot(store.read())
    fun save(trip: Trip?, tripId: String, expectedHeads: Set<String>): JournalSnapshot {
        trip?.validate()
        val fresh = read()
        val heads = fresh.heads(tripId).map { it.id }.toSet()
        require(heads == expectedHeads) { "This swim changed on another device. Close it and review the latest version before saving." }
        require(tripId !in fresh.conflicts) { "Resolve this swim's sync conflict in Storage first." }
        store.append(SwimRevision(UUID.randomUUID().toString(), tripId, heads, trip))
        return read()
    }
    fun resolve(tripId: String, chosen: SwimRevision, expectedHeads: Set<String>): JournalSnapshot {
        val fresh = read()
        val heads = fresh.heads(tripId)
        require(heads.map { it.id }.toSet() == expectedHeads && chosen in heads) { "More changes arrived. Review the conflict again." }
        store.append(SwimRevision(UUID.randomUUID().toString(), tripId, expectedHeads, chosen.trip))
        return read()
    }
    fun copyTo(target: RevisionStore): JournalSnapshot {
        val source = read()
        val existing = RevisionCodec.snapshot(target.read()).revisions.associateBy { it.id }
        source.revisions.forEach { r ->
            require(existing[r.id] == null || existing[r.id] == r) { "The destination contains a damaged revision." }
            if (existing[r.id] == null) target.append(r)
        }
        val result = RevisionCodec.snapshot(target.read())
        require(result.revisions.containsAll(source.revisions)) { "Folder copy could not be verified. The original journal is unchanged." }
        return result
    }
    fun import(trips: List<Trip>): JournalSnapshot {
        // Include deletion histories: an old backup must not resurrect a deleted swim.
        val known = read().revisions.map { it.tripId }.toMutableSet()
        trips.forEach { trip -> if (known.add(trip.id)) store.append(RevisionCodec.seed(trip)) }
        return read()
    }
}
