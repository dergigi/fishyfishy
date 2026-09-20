# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- Shorten the German swim navigation label to “Touren” to fit on one line.

## [0.6.0] - 2026-09-20

### Added
- Add a creature directly to the last logged swim from its detail page, with the destination shown and duplicate sightings prevented.
- Explore filters reappear above the gallery when scrolling upward and hide when scrolling downward.

### Fixed
- Preserve Explore's gallery scroll position, search and filters when returning from a creature page or another screen.

## [0.5.0] - 2026-09-19

### Added
- A flag dropdown beside Info to switch the entire app between English, Portuguese and German, including guide lessons, clues, dates and read-aloud.
- German and Portuguese YouTube selections that follow the app language, with related species and broader habitats labelled.
- Light and dark appearance that follows the device’s system theme, including cards, controls and the swim date picker.

## [0.4.0] - 2026-09-19

### Added
- Watch together cards on creature details, linking directly to 14 selected YouTube documentaries and short science films across all 47 guide entries.
- Film titles, publishers and clear labels for related animals or broader coastal habitats.

## [0.3.2] - 2026-09-19

### Changed
- Use icon-only speech feedback: a loading spinner and animated sound bars, with status words reserved for screen readers.

## [0.3.1] - 2026-09-19

### Fixed
- Speech buttons immediately show Starting and Playing feedback and ignore repeated taps on the active name or lesson.
- Speech waits for engine initialization, handles errors and timeouts, and stops when leaving the screen or backgrounding the app.

## [0.3.0] - 2026-09-19

### Added
- Open guide photos full-screen with pinch zoom, panning, double-tap zoom and accessible zoom/reset controls.
- Thirty coastal guide entries with licensed photos, multilingual names, identification clues and child-friendly lessons, bringing the guide to 47 entries.
- Puffers, morays, blennies, seabreams, trumpetfish, crabs, shrimp, limpets, anemones, fireworms, a hermit crab, a sponge and more urchins and sea stars.
- Sand, Shell and Legs filters, plus documented Madeira coverage and identification limits.

## [0.2.0] - 2026-09-19

### Added
- Choose a journal folder using Android's folder picker, for automatic Syncthing syncing between devices.
- Immutable swim history with explicit resolution of simultaneous offline edits and synced deletions.
- Five guide entries: Mediterranean damselfish (lookalike comparison), Azores chromis, bogue, sand smelt and European sardine.
- Young/adult photo comparisons, extra blue-marking photos, and Blue and Schools filters.
- Folder setup instructions and storage integrity checks.

## [0.1.0] - 2026-09-19

### Added
- Native Android app foundation for phones and tablets.
- Offline photo field guide to 12 Madeira fish and ocean creatures, with identification clues and learning prompts.
- English, Portuguese, German and scientific names, with offline pronunciation for spoken languages.
- Local swim journal with dates, places, duration, notes and uncertain sightings.
- Personal discovery collection, photo identification game, and journal backup export and restore.
- Signed Android releases and Zapstore publishing under the existing publisher identity.

[Unreleased]: https://github.com/dergigi/fishyfishy/compare/v0.6.0...HEAD
[0.6.0]: https://github.com/dergigi/fishyfishy/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/dergigi/fishyfishy/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/dergigi/fishyfishy/compare/v0.3.2...v0.4.0
[0.3.2]: https://github.com/dergigi/fishyfishy/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/dergigi/fishyfishy/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/dergigi/fishyfishy/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/dergigi/fishyfishy/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/dergigi/fishyfishy/releases/tag/v0.1.0
