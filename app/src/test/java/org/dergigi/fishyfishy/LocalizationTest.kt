package org.dergigi.fishyfishy

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LocalizationTest {
    private fun catalog(language: String) = File("src/main/assets/i18n/$language.json").readText()
    private fun keys(json: String) = JSONObject(json).keys().asSequence().toSet()

    @Test fun everyGuideDescriptionIsTranslatedWithoutChangingJournalIdentity() {
        for (language in listOf("pt", "de")) {
            val raw = catalog(language)
            val keys = keys(raw)
            val strings = AppStrings.parse(language, raw)
            for (species in guide) {
                val fields = listOf(species.clues, species.fact, species.habitat, species.mission, species.photoLabel) +
                    species.otherPhotos.map { it.label } + listOfNotNull(species.comparisonNote)
                fields.forEach { assertTrue("Missing $language translation: $it", it in keys) }
                val localized = strings.species(species)
                assertEquals(species.id, localized.id)
                assertEquals(species.scientific, localized.scientific)
                assertEquals(species.name, localized.name)
                assertEquals(species.portuguese, localized.portuguese)
                assertEquals(species.german, localized.german)
                assertEquals(species.tags, localized.tags)
                assertEquals(species.group, localized.group)
                assertNotEquals(species.fact, localized.fact)
            }
        }
    }

    @Test fun translationCatalogsCoverUiCallsAndPreserveFormatArguments() {
        val portuguese = keys(catalog("pt"))
        val german = keys(catalog("de"))
        assertEquals(portuguese, german)
        val call = Regex("""strings\(("(?:[^"\\]|\\.)*")""")
        for (file in listOf("MainActivity.kt", "Speaker.kt", "PhotoViewer.kt")) {
            val source = File("src/main/java/org/dergigi/fishyfishy/$file").readText()
            call.findAll(source).forEach {
                val key = JSONArray("[${it.groupValues[1]}]").getString(0)
                assertTrue("Untranslated UI call: $key", key in portuguese)
            }
        }
        val placeholders = Regex("%[ds]")
        for (language in listOf("pt", "de")) {
            val data = JSONObject(catalog(language))
            val strings = AppStrings.parse(language, catalog(language))
            portuguese.forEach { key ->
                val expected = placeholders.findAll(key).map { it.value }.toList()
                val actual = placeholders.findAll(data.getString(key)).map { it.value }.toList()
                assertEquals("Mismatched placeholders: $language $key", expected, actual)
                val args = expected.map { if (it == "%d") 3 else "Madeira" }.toTypedArray<Any>()
                assertTrue(strings(key, *args).isNotBlank())
            }
        }
    }

    @Test fun localizedSearchFindsNamesCluesAndTags() {
        val german = AppStrings.parse("de", catalog("de"))
        val portuguese = AppStrings.parse("pt", catalog("pt"))
        val sardine = guide.first { it.id == "sardine" }
        assertTrue(german.matches(german.species(sardine), "Schwärme"))
        assertTrue(portuguese.matches(portuguese.species(sardine), "prateado"))
        assertTrue(german.matches(german.species(sardine), "Sardina pilchardus"))
        assertTrue(german.matches(german.species(sardine), "silvery"))
        assertEquals("1 Tier", german.creatures(1))
        assertEquals("2 Tiere", german.creatures(2))
        assertEquals("1 animal", portuguese.creatures(1))
        assertEquals("PT", portuguese.locale.country)
    }

    @Test fun everyCreatureHasAFilmInEachSelectedLanguage() {
        for (language in listOf("en", "pt", "de")) {
            for (species in guide) {
                val film = requireNotNull(filmFor(species.id, language))
                assertEquals(language, film.language)
                assertTrue(film.videoId.matches(Regex("[A-Za-z0-9_-]{11}")))
                assertTrue(film.title.isNotBlank() && film.note.isNotBlank())
                if (language != "en") assertNotEquals(guideFilms[species.id]?.videoId, film.videoId)
            }
        }
        assertNull(filmFor("missing_species", "de"))
    }

    @Test fun providerDiagnosticsHaveALocalizedFallback() {
        val strings = AppStrings.parse("de", catalog("de"))
        assertEquals("Wähle heute oder ein früheres Datum.", strings.error("Choose today or an earlier date."))
        assertEquals(strings("Please check the file or storage and try again."), strings.error("Provider unknown failure"))
        assertEquals("Tour gespeichert. Noch ein kleines Abenteuer!", strings.error("Swim saved. Another little adventure!"))
    }
}
