package org.dergigi.fishyfishy

import androidx.compose.runtime.staticCompositionLocalOf
import org.json.JSONObject
import java.util.Locale

/** Bundled translations. English keys keep the guide's source text next to its translations. */
class AppStrings(val language: String, private val translations: Map<String, String> = emptyMap()) {
    val locale: Locale = Locale.forLanguageTag(when (language) { "pt" -> "pt-PT"; "de" -> "de-DE"; else -> "en-GB" })
    operator fun invoke(english: String, vararg arguments: Any): String {
        val translated = translations[english] ?: english
        return if (arguments.isEmpty()) translated else String.format(locale, translated, *arguments)
    }
    fun error(message: String): String = if (language == "en" || translations.containsKey(message)) invoke(message)
        else invoke("Please check the file or storage and try again.")
    fun creatures(count: Int): String = if (count == 1) invoke("1 creature") else invoke("%d creatures", count)
    fun matches(species: Species, query: String): Boolean = species.matches(query) ||
        guide.first { it.id == species.id }.matches(query) ||
        species.tags.any { normalized(invoke(it)).contains(normalized(query.trim())) }
    fun species(original: Species) = original.copy(
        clues = invoke(original.clues), fact = invoke(original.fact), habitat = invoke(original.habitat),
        photoDescription = original.photoDescription?.let { invoke(it) },
        mission = invoke(original.mission), photoLabel = invoke(original.photoLabel),
        otherPhotos = original.otherPhotos.map { it.copy(label = invoke(it.label), description = it.description?.let { text -> invoke(text) }) },
        comparisonNote = original.comparisonNote?.let { invoke(it) },
    )
    companion object {
        fun parse(language: String, json: String): AppStrings {
            val data = JSONObject(json)
            return AppStrings(language, data.keys().asSequence().associateWith { data.getString(it) })
        }
    }
}

val LocalStrings = staticCompositionLocalOf { AppStrings("en") }
val LocalGuide = staticCompositionLocalOf { guide }
