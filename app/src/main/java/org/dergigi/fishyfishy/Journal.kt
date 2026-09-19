package org.dergigi.fishyfishy

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val date: String, val place: String, val minutes: Int, val notes: String,
    val sightings: Set<String>, val uncertain: Set<String> = emptySet(),
) {
    fun validate(allowFuture: Boolean = false) {
        require(id.isNotBlank() && id.length <= 100) { "Invalid swim ID." }
        LocalDate.parse(date)
        require(allowFuture || !LocalDate.parse(date).isAfter(LocalDate.now())) { "Choose today or an earlier date." }
        require(place.isNotBlank() && place.length <= 150) { "Add a place (up to 150 characters)." }
        require(minutes in 1..600) { "Duration must be between 1 and 600 minutes." }
        require(notes.length <= 10000) { "Notes are too long." }
        require(sightings.all { id -> guide.any { it.id == id } }) { "This backup needs a newer field guide." }
        require(sightings.containsAll(uncertain)) { "An uncertain sighting must belong to the swim." }
    }
}

object JournalCodec {
    fun encode(trips: List<Trip>): String = JSONObject().put("format", "fishyfishy").put("version", 1)
        .put("trips", JSONArray().apply { trips.forEach { t ->
            put(JSONObject().put("id", t.id).put("date", t.date).put("place", t.place)
                .put("minutes", t.minutes).put("notes", t.notes)
                .put("sightings", JSONArray(t.sightings.toList())).put("uncertain", JSONArray(t.uncertain.toList())))
        } }).toString(2)
    fun decode(raw: String): List<Trip> {
        require(raw.length <= 5_000_000) { "This backup is too large." }
        val root = JSONObject(raw)
        require(root.getString("format") == "fishyfishy" && root.getInt("version") == 1) { "Unsupported backup format." }
        val array = root.getJSONArray("trips")
        require(array.length() <= 10000) { "Too many swims in this backup." }
        fun JSONArray.strings() = (0 until length()).map { getString(it) }.toSet()
        val trips = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Trip(o.getString("id"), o.getString("date"), o.getString("place"), o.getInt("minutes"),
                o.getString("notes"), o.getJSONArray("sightings").strings(), o.getJSONArray("uncertain").strings()).also { it.validate(allowFuture = true) }
        }
        require(trips.map { it.id }.distinct().size == trips.size) { "Duplicate swim IDs in backup." }
        return trips.sortedByDescending { it.date }
    }
    // Import adds missing swims and never silently overwrites a local edit.
    fun merge(local: List<Trip>, incoming: List<Trip>): List<Trip> =
        (local + incoming.filter { t -> local.none { it.id == t.id } }).sortedByDescending { it.date }
}
