# Languages

The flag beside Info opens English, Português and Deutsch choices. Selection
persists in local preferences and recreates the activity with a localized Android
configuration, so native date-picker and Compose accessibility strings follow it.
Compose saveable navigation and drafts survive recreation; the journal view model
and file format are unchanged. Language is a device preference, not journal data.

`AppStrings` reads bundled `assets/i18n/pt.json` or `de.json`. English source text
is the lookup key. Add a matching translation to both catalogs when introducing
user-facing text. Parameterized strings use Java format placeholders (`%d`, `%s`);
placeholder order and types must match. Use `strings.error` for storage errors to
avoid displaying untranslated provider diagnostics in a non-English interface.

`LocalGuide` contains localized copies of the reference data. Only descriptions,
lessons, habitats, missions and photo captions change. Species IDs, filter keys,
scientific names, four common/scientific name fields and photo attribution remain
stable. Names in all three languages remain visible on detail pages. Search
includes localized tags and clues as well as the original names and English clues.

Read-aloud for lessons uses the selected language. The three name buttons keep
using their own language. Localized film maps are independent of the UI catalog;
see DOCUMENTARIES.md for selections and verification.

`LocalizationTest` checks guide translation coverage, format placeholders, catalog
keys used by the UI, stable journal IDs, localized searches and film language
coverage for every species. No live internet access is needed to run these checks.
