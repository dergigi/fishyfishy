# FishyFishy

A little journal for big underwater discoveries. An offline snorkeling journal and visual Madeira field guide for curious kids and their grown-ups.

[Install from Zapstore](https://zapstore.dev/apps/org.dergigi.fishyfishy) · [Download the APK](https://github.com/dergigi/fishyfishy/releases/latest)

Native Android, built with Kotlin and Jetpack Compose. Package: `org.dergigi.fishyfishy`.

## Build

Requires JDK 17 and Android SDK 35.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

## Versioning

[Semantic Versioning](https://semver.org/), [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

## What you can do

- Browse 47 fish and ocean creatures, including Madeira species and a blue-striped lookalike with bundled reference photos.
- Switch the whole app between English, Portuguese and German using the flag menu beside Info; scientific names remain alongside the common names.
- Read and hear guide lessons in your selected language using installed offline Android voices.
- Follow the device’s light or dark theme automatically.
- Log a swim's date, location, duration, notes and sightings; mark guesses as uncertain.
- Add a forgotten creature to your last logged swim directly from its detail page.
- Revisit, edit and delete swims; grow a collection of confirmed discoveries.
- Play a gentle photo identification game together.
- Open a documentary or educational short in the selected language from each creature page; broader habitat films are labelled clearly. Portuguese shorts use on-screen Portuguese text. See [film selections](docs/DOCUMENTARIES.md).
- Choose a journal folder and sync it between devices with Syncthing; resolve simultaneous edits in the app.
- Swipe through captioned reference photos, including male/female parrotfish and wrasse, juveniles and a Madeira salema school; tap to open full-screen and pinch or double-tap to zoom.
- Find sandy-bottom fish, shells and many-legged critters with visual clue filters.
- Export and restore a JSON journal backup through Android's document picker.

Android 8.0 or newer. Phone layouts use bottom navigation; wider tablet windows use a navigation rail and adaptive photo grids. The app provides a curated coastal reference guide, not automated photo identification.

## Privacy

No network permission, analytics, account, GPS access or advertising. Choose a local folder in Info → Journal storage to keep all journal data on the filesystem and sync it with Syncthing. See [folder setup and conflict handling](docs/STORAGE.md). Until a folder is selected, the journal stays in app-private storage. Android automatic backup is disabled; preserve your chosen folder or export a backup before uninstalling. Backups contain swim locations and notes, so save them somewhere appropriate. Restoring adds missing swim IDs and retains existing local edits.

The app requests only offline text-to-speech voices. Voice availability depends on the device. Source links open in the user's browser only when tapped. Documentary links open YouTube or a browser and require internet there; the guide and journal remain offline.

## Sources and image licences

See [guide references](docs/GUIDE-SOURCES.md) and the bundled [photo credits](app/src/main/assets/photo-credits.json). Each species detail page includes its reference, photographer, licence and source link. Photos retain their own CC BY / CC BY-SA licences; the MIT licence applies to the app code and original artwork.

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

The script uses `zsp` (and `nak` for first-time certificate linking), checks the Android certificate's identity link, then publishes the exact signed local APK with the committed metadata and changelog. `SIGN_WITH` must be configured in the environment or the supplied file. For the first publication, add `--link-identity` to create the certificate proof using the configured Android signing key. Your Nostr bunker must be online and may ask you to approve signing requests. See [Zapstore publishing documentation](https://zapstore.dev/docs/publish).

No emulator testing was performed for the initial release. Build, JVM tests, Android Lint and APK signature checks are used before publication. The narrow `NullSafeMutableLiveData` lint exclusion matches Boris's workaround for a Lifecycle/AGP detector incompatibility; the app uses Compose state rather than LiveData.
