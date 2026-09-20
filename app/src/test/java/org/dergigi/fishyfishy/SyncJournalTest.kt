package org.dergigi.fishyfishy

import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

class SyncJournalTest {
    @get:Rule val temp = TemporaryFolder()
    private fun trip(id: String = "swim") = Trip(id, "2026-01-02", "Garajau", 30, "A blue fish", setOf("wrasse"))
    private class MemoryStore : RevisionStore {
        val files = mutableMapOf<String, SwimRevision>()
        var fail = false
        override fun read() = files.values.toList()
        override fun append(revision: SwimRevision) {
            if (fail) error("Disk full")
            require(files[revision.id] == null || files[revision.id] == revision)
            files[revision.id] = revision
        }
    }
    private fun heads(s: JournalSnapshot, id: String = "swim") = s.heads(id).map { it.id }.toSet()
    @Test fun quickSightingPreservesSwimDetailsAndSyncsToAnotherDevice() {
        val a = MemoryStore(); val b = MemoryStore(); val journal = SyncJournal(a)
        val original = trip().copy(uncertain = setOf("wrasse"))
        val base = journal.save(original, original.id, emptySet())
        journal.copyTo(b)
        val updated = journal.addSighting(original.id, "octopus", heads(base))
        assertEquals(original.copy(sightings = setOf("wrasse", "octopus")), updated.trips.single())
        assertEquals(updated.trips, journal.copyTo(b).trips)
        assertTrue(journal.copyTo(b).conflicts.isEmpty())
    }
    @Test fun quickSightingDoesNotDuplicateOrConfirmAnUncertainSighting() {
        val store = MemoryStore(); val journal = SyncJournal(store)
        val original = trip().copy(uncertain = setOf("wrasse"))
        val base = journal.save(original, original.id, emptySet())
        repeat(5) { journal.addSighting(original.id, "wrasse", heads(base)) }
        assertEquals(1, store.read().size)
        assertEquals(original, journal.read().trips.single())
    }
    @Test fun quickSightingRejectsAChangedOrDeletedSwim() {
        val store = MemoryStore(); val journal = SyncJournal(store)
        val base = journal.save(trip(), "swim", emptySet())
        val edited = journal.save(trip().copy(notes = "New notes from tablet"), "swim", heads(base))
        assertThrows(IllegalArgumentException::class.java) { journal.addSighting("swim", "octopus", heads(base)) }
        assertEquals("New notes from tablet", journal.read().trips.single().notes)
        val deleted = journal.save(null, "swim", heads(edited))
        assertThrows(IllegalArgumentException::class.java) { journal.addSighting("swim", "octopus", heads(edited)) }
        assertThrows(IllegalArgumentException::class.java) { journal.addSighting("swim", "octopus", heads(deleted)) }
        assertTrue(journal.read().trips.isEmpty())
        assertEquals(3, store.read().size)
    }
    @Test fun quickSightingRequiresConflictResolution() {
        val a = MemoryStore(); val b = MemoryStore(); val ja = SyncJournal(a); val jb = SyncJournal(b)
        val base = ja.save(trip(), "swim", emptySet()); ja.copyTo(b)
        ja.save(trip().copy(notes = "Phone"), "swim", heads(base))
        jb.save(trip().copy(notes = "Tablet"), "swim", heads(base))
        val conflict = ja.copyTo(b)
        assertThrows(IllegalArgumentException::class.java) { jb.addSighting("swim", "octopus", heads(conflict)) }
        assertEquals(conflict.revisions, jb.read().revisions)
    }
    @Test fun differentDevicesAddIndependentSwimsWithoutOverwriting() {
        val a = MemoryStore(); val b = MemoryStore()
        SyncJournal(a).save(trip("one"), "one", emptySet())
        SyncJournal(b).save(trip("two"), "two", emptySet())
        val result = SyncJournal(a).copyTo(b)
        assertEquals(setOf("one", "two"), result.trips.map { it.id }.toSet())
        assertTrue(result.conflicts.isEmpty())
    }
    @Test fun simultaneousEditsPreserveBothAndExplicitResolutionConverges() {
        val a = MemoryStore(); val b = MemoryStore(); val ja = SyncJournal(a); val jb = SyncJournal(b)
        val base = ja.save(trip(), "swim", emptySet()); ja.copyTo(b)
        ja.save(trip().copy(notes = "Phone edit"), "swim", heads(base))
        jb.save(trip().copy(notes = "Tablet edit"), "swim", heads(base))
        val conflict = ja.copyTo(b)
        assertEquals(2, conflict.conflicts.getValue("swim").size)
        val choice = conflict.heads("swim").first { it.trip?.notes == "Tablet edit" }
        val resolved = jb.resolve("swim", choice, heads(conflict))
        assertTrue(resolved.conflicts.isEmpty()); assertEquals("Tablet edit", resolved.trips.single().notes)
        assertEquals(resolved.trips, jb.copyTo(a).trips)
        assertEquals(4, a.read().size) // Neither losing history nor the original is removed.
    }
    @Test fun deletionDoesNotResurrectWhenOldDeviceSyncs() {
        val a = MemoryStore(); val b = MemoryStore(); val ja = SyncJournal(a)
        val base = ja.save(trip(), "swim", emptySet()); ja.copyTo(b)
        ja.save(null, "swim", heads(base))
        assertTrue(SyncJournal(b).copyTo(a).trips.isEmpty())
        assertTrue(ja.copyTo(b).trips.isEmpty())
        assertTrue(ja.import(listOf(trip())).trips.isEmpty())
    }
    @Test fun deletionAndOfflineEditAreAConflict() {
        val a = MemoryStore(); val b = MemoryStore(); val ja = SyncJournal(a); val jb = SyncJournal(b)
        val base = ja.save(trip(), "swim", emptySet()); ja.copyTo(b)
        ja.save(null, "swim", heads(base)); jb.save(trip().copy(notes = "Offline"), "swim", heads(base))
        val both = ja.copyTo(b)
        assertEquals("Offline", both.trips.single().notes)
        assertEquals(2, both.conflicts.getValue("swim").size)
        val deleted = jb.resolve("swim", both.heads("swim").first { it.trip == null }, heads(both))
        assertTrue(deleted.trips.isEmpty()); assertTrue(deleted.conflicts.isEmpty())
    }
    @Test fun staleOpenEditorCannotOverwriteAnIncomingEdit() {
        val store = MemoryStore(); val journal = SyncJournal(store)
        val base = journal.save(trip(), "swim", emptySet())
        journal.save(trip().copy(notes = "New"), "swim", heads(base))
        assertTrue(runCatching { journal.save(trip().copy(notes = "Stale"), "swim", heads(base)) }.isFailure)
        assertEquals("New", journal.read().trips.single().notes)
    }
    @Test fun duplicateImportsAreIdempotentAndReorderedSyncConverges() {
        val store = MemoryStore(); val j = SyncJournal(store)
        j.import(listOf(trip())); j.import(listOf(trip()))
        val base = j.read(); j.save(trip().copy(notes = "New"), "swim", heads(base))
        val events = store.read()
        assertEquals(j.read().trips, RevisionCodec.snapshot(events.reversed() + events).trips)
        // Receiving a child before its parent is safe; the late parent never resurrects old data.
        val child = events.first { it.parents.isNotEmpty() }
        assertEquals("New", RevisionCodec.snapshot(listOf(child)).trips.single().notes)
    }
    @Test fun incompleteWritesAndDiskFailuresNeverReplaceCommittedFiles() {
        val dir = temp.newFolder(); val store = LocalRevisionStore(dir); val journal = SyncJournal(store)
        val base = journal.save(trip(), "swim", emptySet())
        File(dir, ".pending-test").writeText("{half written")
        assertEquals(base.trips, journal.read().trips)
        val memory = MemoryStore(); memory.append(base.revisions.single()); memory.fail = true
        assertTrue(runCatching { SyncJournal(memory).save(trip().copy(notes = "Lost"), "swim", heads(base)) }.isFailure)
        assertEquals(base.trips, SyncJournal(memory).read().trips)
    }
    @Test fun corruptFilesFailReadRatherThanSilentlyDroppingSwims() {
        val dir = temp.newFolder(); val store = LocalRevisionStore(dir)
        store.append(RevisionCodec.seed(trip()))
        File(dir, "swim-broken.json").writeText("{half written")
        assertTrue(runCatching { SyncJournal(store).read() }.isFailure)
    }
    @Test fun checksumsAndUnknownFormatsAreRejected() {
        val encoded = RevisionCodec.encode(RevisionCodec.seed(trip()))
        assertEquals(trip(), RevisionCodec.decode(encoded).trip)
        val damaged = JSONObject(encoded).put("sha256", "bad").toString()
        assertTrue(runCatching { RevisionCodec.decode(damaged) }.isFailure)
        assertTrue(runCatching { RevisionCodec.decode(JSONObject(encoded).put("version", 99).toString()) }.isFailure)
    }
    @Test fun failedFolderMigrationRetainsSourceAndRetryIsSafe() {
        val a = MemoryStore(); val b = MemoryStore(); val journal = SyncJournal(a)
        journal.import(listOf(trip("one"), trip("two")))
        b.fail = true
        assertTrue(runCatching { journal.copyTo(b) }.isFailure)
        assertEquals(2, journal.read().trips.size)
        b.fail = false
        journal.copyTo(b); journal.copyTo(b)
        assertEquals(2, b.read().size)
    }
    @Test fun concurrentIdenticalMigrationSeedsDoNotCreateConflicts() {
        val r = RevisionCodec.seed(trip())
        val duplicate = r.copy(id = UUID.randomUUID().toString())
        val s = RevisionCodec.snapshot(listOf(r, duplicate))
        assertTrue(s.conflicts.isEmpty()); assertEquals(1, s.trips.size)
    }
    @Test fun cyclesAndCrossSwimReferencesAreRejected() {
        val a = RevisionCodec.seed(trip("one")); val b = RevisionCodec.seed(trip("two"))
        assertTrue(runCatching { RevisionCodec.snapshot(listOf(a, b.copy(parents = setOf(a.id)))) }.isFailure)
        val c = a.copy(id = UUID.randomUUID().toString(), parents = setOf(a.id))
        assertTrue(runCatching { RevisionCodec.snapshot(listOf(a.copy(parents = setOf(c.id)), c)) }.isFailure)
    }
    @Test fun duplicateRevisionIdsWithDifferentPayloadsAreRejected() {
        val a = RevisionCodec.seed(trip()); val b = a.copy(trip = trip().copy(notes = "Changed"))
        assertTrue(runCatching { RevisionCodec.snapshot(listOf(a, b)) }.isFailure)
    }
    @Test fun readingDoesNotRejectASwimFromADeviceOneDayAhead() {
        val tomorrow = trip().copy(date = LocalDate.now().plusDays(1).toString())
        assertEquals(tomorrow, RevisionCodec.decode(RevisionCodec.encode(RevisionCodec.seed(tomorrow))).trip)
        assertTrue(runCatching { tomorrow.validate() }.isFailure)
    }
}
