# FishyFishy

A little journal for big underwater discoveries. An offline snorkeling journal and visual Madeira field guide for curious kids and their grown-ups.

Native Android, built with Kotlin and Jetpack Compose. Package: `org.dergigi.fishyfishy`.

## Build

Requires JDK 17 and Android SDK 35.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

## Versioning

[Semantic Versioning](https://semver.org/), [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

## What you can do

- Browse 12 Madeira fish and ocean creatures with bundled reference photos.
- Switch guide names between English, Portuguese and German; see all names and scientific names on detail pages.
- Hear names with installed offline Android voices. Read short English lessons and identification clues.
- Log a swim's date, location, duration, notes and sightings; mark guesses as uncertain.
- Revisit, edit and delete swims; grow a collection of confirmed discoveries.
- Play a gentle photo identification game together.
- Export and restore a JSON journal backup through Android's document picker.

Android 8.0 or newer. Phone layouts use bottom navigation; wider tablet windows use a navigation rail and adaptive photo grids. The interface and lessons are currently English. The app provides a small reference guide, not automated photo identification.

## Privacy

No network permission, analytics, account, GPS access or advertising. The journal is stored in an atomic app-private file. Android automatic backup is disabled; export a backup from the info screen before uninstalling or changing devices. Backups contain swim locations and notes, so save them somewhere appropriate. Restoring adds missing swim IDs and retains existing local edits.

The app requests only offline text-to-speech voices. Voice availability depends on the device. Source links open in the user's browser only when tapped.

## Sources and image licences

See [guide references](docs/GUIDE-SOURCES.md) and the bundled [photo credits](app/src/main/assets/photo-credits.json). Each species detail page includes its reference, photographer, licence and source link. Photos retain their own CC BY-SA licences; the MIT licence applies to the app code and original artwork.

## Signed releases and Zapstore

The application ID is `org.dergigi.fishyfishy`. Signing follows Boris's local configuration. Supply these through environment variables or gitignored `local.properties`:

```properties
OEM_STORE_FILE=/absolute/path/to/keystore.jks
OEM_STORE_PASSWORD=...
OEM_KEY_ALIAS=...
OEM_KEY_PASSWORD=...
```

```sh
./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
```

Keep signing credentials private and retain the signing key for future updates. Without all signing properties, Gradle creates an unsigned release which cannot be published as an installable update.

Create a GitHub release for the version with the signed APK attached, then publish using the metadata in [`zapstore.yaml`](zapstore.yaml):

```sh
python3 scripts/zapstore-publish.py
# Or use an existing publisher's ignored environment file:
python3 scripts/zapstore-publish.py --env-file /path/to/publisher/.env
```

The script uses `zsp`, checks the Android certificate's identity link, then publishes the latest GitHub release. `SIGN_WITH` must be configured in the environment or the supplied file. A new signing key needs a one-time `zsp identity --link-key` setup. See [Zapstore publishing documentation](https://zapstore.dev/docs/publish).

No emulator testing was performed for the initial release. Build, JVM tests, Android Lint and APK signature checks are used before publication. The narrow `NullSafeMutableLiveData` lint exclusion matches Boris's workaround for a Lifecycle/AGP detector incompatibility; the app uses Compose state rather than LiveData.
