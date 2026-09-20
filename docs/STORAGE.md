# Syncing your journal

1. Open **Info → Journal storage → Choose folder** and select a local folder, such as `Documents/FishyFishy`.
2. Share that whole folder with Syncthing using send-and-receive syncing.
3. Install FishyFishy 0.2.0 or later on each device and choose its corresponding synced folder in the app.

Existing swims are copied into the chosen folder and verified before switching. Originals remain in the previous location. The app reads directly from the selected folder, checking when opened and every five seconds while in the foreground. Use Refresh to check immediately. Syncthing handles transfer separately; FishyFishy needs no network permission. Keep every device on the same app version when adding newly introduced species.

All swim dates, places, notes, sightings and edit history live in that folder as `swim-*.json` files. The folder access grant and display preferences are device-specific; the guide and reference photos are bundled in the APK. Until you choose a folder, journal files remain in app-private storage and are removed when uninstalling.

Each edit creates an immutable revision. Deletions create records too, so an offline device cannot silently resurrect a deleted swim. Sync the entire folder; do not edit or remove individual files. Temporary `.pending-*` files are ignored. If two devices edit the same swim offline, Info shows both versions and asks which to keep; that choice then syncs. Conflicting delete/edit versions also need a choice. No device's clock decides whose edit wins.

If access is lost or files are damaged or missing, writes stop and the app keeps its last successfully read view. Reconnect the original folder and let syncing finish. Changing folders copies the current history without deleting the source. A writable folder with file rename support is required; a local Documents folder is recommended.

Sync is not a backup. Enable Syncthing file versioning or back up the folder separately. Deleted swims remain in revision history. JSON export/import is still available as a portable snapshot, but does not preserve edit history and requires conflicts to be resolved before export. Import retains existing swims and deletions. Android automatic backup is disabled.

## Adding a remembered sighting

Creature detail pages show an “Add to last swim” shortcut with the destination’s
place and date. The app remembers the last swim created on this device, even
when logging an older date; editing an existing swim does not change that choice.
For an existing installation, or if that swim has been deleted, it uses the most
recent swim by date. The last-used swim ID is a local UI preference, alongside the
folder selection, and does not change the journal file format.

The shortcut adds only the selected creature. Existing notes, dates, durations
and uncertain sightings remain intact. Repeat additions do not create another
sighting or turn an uncertain sighting into a confirmed one. The same revision
checks as the full editor reject stale or conflicted updates and deleted swims.
