package org.dergigi.fishyfishy

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class JournalModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("storage", 0)
    private val resolver = application.contentResolver
    private val localStore = LocalRevisionStore(File(application.filesDir, "swims"))
    var folderUri by mutableStateOf(prefs.getString("folder", null)); private set
    var folderLabel by mutableStateOf(prefs.getString("folderLabel", null)); private set
    private var store: RevisionStore = folderUri?.let { FolderRevisionStore(resolver, Uri.parse(it)) } ?: localStore
    private var snapshot = JournalSnapshot(emptyList())
    var trips by mutableStateOf<List<Trip>>(emptyList()); private set
    var conflicts by mutableStateOf<Map<String, List<SwimRevision>>>(emptyMap()); private set
    var loading by mutableStateOf(true); private set
    var busy by mutableStateOf(false); private set
    var loadError by mutableStateOf<String?>(null); private set
    var operationError by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null)
    var lastChecked by mutableStateOf<String?>(null); private set
    init {
        viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    if (folderUri == null && !prefs.getBoolean("migrated", false)) {
                        val old = AtomicFile(File(application.filesDir, "journal.json"))
                        if (old.baseFile.exists() || File(old.baseFile.path + ".bak").exists()) {
                            val legacy = JournalCodec.decode(old.openRead().bufferedReader().use { it.readText() })
                            SyncJournal(localStore).import(legacy)
                        }
                        check(prefs.edit().putBoolean("migrated", true).commit())
                    }
                    SyncJournal(store).read()
                }
                accept(loaded)
            } catch (_: Exception) { loadError = unavailable() }
            finally { loading = false }
        }
    }
    private fun unavailable() = if (folderUri != null)
        "Your journal folder is unavailable or still syncing. No files have been replaced. Check folder access, let sync finish, then refresh or reselect the same folder."
        else "Your journal could not be read. Its files have been kept. Please check your storage before saving more swims."
    private fun accept(next: JournalSnapshot) {
        snapshot = next; trips = next.trips; conflicts = next.conflicts; loadError = null
        lastChecked = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
    }
    fun headIds(id: String) = snapshot.heads(id).map { it.id }.toSet()
    fun clearOperationError() { operationError = null }
    fun refresh() {
        if (loading || busy) return
        busy = true
        viewModelScope.launch {
            try {
                val next = withContext(Dispatchers.IO) { SyncJournal(store).read() }
                // An incomplete sync or disconnected provider must not make known memories disappear.
                require(next.revisions.containsAll(snapshot.revisions))
                accept(next)
            } catch (_: Exception) { loadError = unavailable() }
            finally { busy = false }
        }
    }
    fun selectFolder(uri: Uri) {
        if (loading || busy) return
        busy = true; operationError = null
        viewModelScope.launch {
            try {
                val selection = withContext(Dispatchers.IO) {
                    resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    val target = FolderRevisionStore(resolver, uri)
                    target.verifyWritable()
                    val next = if (uri.toString() == folderUri) SyncJournal(target).read() else SyncJournal(store).copyTo(target)
                    require(next.revisions.containsAll(snapshot.revisions)) { "Some existing swims are missing. Let the folder finish syncing first." }
                    val label = target.displayName()
                    // Switch only after all records have been copied and read back successfully.
                    check(prefs.edit().putString("folder", uri.toString()).putString("folderLabel", label).commit())
                    Triple(target, next, label)
                }
                store = selection.first; folderUri = uri.toString(); folderLabel = selection.third
                accept(selection.second)
                message = "Journal folder connected. Sync this folder on your other devices too."
            } catch (_: Exception) {
                operationError = "Couldn't connect that folder. Your current journal is unchanged. Choose a writable local folder, or reselect your existing folder to restore access."
                message = operationError
            } finally { busy = false }
        }
    }
    fun save(trip: Trip, expectedHeads: Set<String>, done: () -> Unit) {
        if (busy || loading || loadError != null) return
        mutate("Swim saved. Another little adventure!", done) { SyncJournal(store).save(trip, trip.id, expectedHeads) }
    }
    fun delete(trip: Trip, expectedHeads: Set<String>, done: () -> Unit) {
        if (busy || loading || loadError != null) return
        mutate("Swim deleted. The deletion will sync to your other devices.", done) { SyncJournal(store).save(null, trip.id, expectedHeads) }
    }
    fun resolve(tripId: String, chosen: SwimRevision, expectedHeads: Set<String>) {
        if (busy || loading || loadError != null) return
        mutate("Your choice has been saved and will sync.", {}) { SyncJournal(store).resolve(tripId, chosen, expectedHeads) }
    }
    private fun mutate(success: String, done: () -> Unit, action: () -> JournalSnapshot) {
        busy = true; operationError = null
        viewModelScope.launch {
            try { accept(withContext(Dispatchers.IO) { action() }); message = success; done() }
            catch (e: Exception) {
                operationError = if (e is IllegalArgumentException) e.message else "Couldn't save to your journal folder. Your draft and existing files are kept. Check storage and try again."
                message = operationError
            } finally { busy = false }
        }
    }
    fun importBackup(read: () -> String) {
        if (busy || loading || loadError != null) return
        mutate("Backup restored. Existing swims and deletions kept.", {}) {
            SyncJournal(store).import(JournalCodec.decode(read()))
        }
    }
    fun exportBackup(write: (String) -> Unit) {
        if (busy || loading || loadError != null) return
        if (conflicts.isNotEmpty()) { message = "Resolve your sync conflicts before exporting a flat journal backup."; return }
        val copy = trips
        busy = true
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { write(JournalCodec.encode(copy)) }; message = "Backup saved." }
            catch (_: Exception) { message = "Couldn't export. Your journal files are unchanged." }
            finally { busy = false }
        }
    }
}
