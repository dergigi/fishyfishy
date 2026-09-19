package org.dergigi.fishyfishy

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class JournalTest {
    private fun swim(id: String = "one") = Trip(id, "2026-01-02", "Garajau 🐟", 25, "Peixe-verde & Spaß!\nTwo friends.", setOf("wrasse", "octopus"), setOf("octopus"))
    @Test fun backupRoundTripPreservesUnicodeAndUncertainSightings() {
        val original = listOf(swim())
        assertEquals(original, JournalCodec.decode(JournalCodec.encode(original)))
    }
    @Test fun restoreKeepsLocalEditsAndAddsMissingSwims() {
        val local = swim().copy(notes = "Updated after checking our photos")
        val merged = JournalCodec.merge(listOf(local), listOf(swim(), swim("two")))
        assertEquals(2, merged.size)
        assertEquals(local, merged.first { it.id == "one" })
    }
    @Test fun repeatedRestoreDoesNotDuplicateMemories() {
        val original = listOf(swim())
        assertEquals(original, JournalCodec.merge(original, original))
    }
    @Test fun emptyJournalRoundTrip() { assertEquals(emptyList<Trip>(), JournalCodec.decode(JournalCodec.encode(emptyList()))) }
    @Test fun rejectsInvalidDatesDurationsAndUnknownCreatures() {
        listOf(swim().copy(date = "not-a-date"), swim().copy(date = LocalDate.now().plusDays(1).toString()),
            swim().copy(minutes = 0), swim().copy(minutes = 601), swim().copy(place = "  "),
            swim().copy(sightings = setOf("made-up-fish")), swim().copy(uncertain = setOf("bream")))
            .forEach { trip -> assertTrue(runCatching { trip.validate() }.isFailure) }
    }
    @Test fun rejectsDuplicateIdsAndUnknownBackupVersions() {
        assertTrue(runCatching { JournalCodec.decode(JournalCodec.encode(listOf(swim(), swim()))) }.isFailure)
        val future = JournalCodec.encode(listOf(swim())).replace("\"version\": 1", "\"version\": 99")
        assertTrue(runCatching { JournalCodec.decode(future) }.isFailure)
    }
    @Test fun multilingualSearchIgnoresAccentsAndCase() {
        val fish = guide.first { it.id == "parrotfish" }
        listOf("BODIAO", "papageifisch", "Sparisoma", "parrotfish").forEach { assertTrue(fish.matches(it)) }
        assertFalse(fish.matches("octopus"))
    }
    @Test fun guideIdsAndNamesAreCompleteAndUnique() {
        assertEquals(guide.size, guide.map { it.id }.toSet().size)
        guide.forEach { assertTrue(listOf(it.name, it.portuguese, it.german, it.scientific, it.clues, it.fact, it.source).none(String::isBlank)) }
    }
}
