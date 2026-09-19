package org.dergigi.fishyfishy

import android.app.Application
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val date: String, val place: String, val minutes: Int, val notes: String,
    val sightings: Set<String>, val uncertain: Set<String> = emptySet(),
) {
    fun validate() {
        require(id.isNotBlank() && id.length <= 100) { "Invalid swim ID." }
        require(!LocalDate.parse(date).isAfter(LocalDate.now())) { "Choose today or an earlier date." }
        require(place.isNotBlank() && place.length <= 150) { "Add a place (up to 150 characters)." }
        require(minutes in 1..600) { "Duration must be between 1 and 600 minutes." }
        require(notes.length <= 10000) { "Notes are too long." }
        require(sightings.all { id -> guide.any { it.id == id } }) { "This backup needs a newer field guide." }
        require(sightings.containsAll(uncertain)) { "An uncertain sighting must belong to the swim." }
    }
}

object JournalCodec {
    fun encode(trips: List<Trip>): String = JSONObject().put("format", "fishyfishy").put("version", 1)
        .put("trips", JSONArray().apply { trips.forEach { t ->
            put(JSONObject().put("id", t.id).put("date", t.date).put("place", t.place)
                .put("minutes", t.minutes).put("notes", t.notes)
                .put("sightings", JSONArray(t.sightings.toList())).put("uncertain", JSONArray(t.uncertain.toList())))
        } }).toString(2)
    fun decode(raw: String): List<Trip> {
        require(raw.length <= 5_000_000) { "This backup is too large." }
        val root = JSONObject(raw)
        require(root.getString("format") == "fishyfishy" && root.getInt("version") == 1) { "Unsupported backup format." }
        val array = root.getJSONArray("trips")
        require(array.length() <= 10000) { "Too many swims in this backup." }
        fun JSONArray.strings() = (0 until length()).map { getString(it) }.toSet()
        val trips = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Trip(o.getString("id"), o.getString("date"), o.getString("place"), o.getInt("minutes"),
                o.getString("notes"), o.getJSONArray("sightings").strings(), o.getJSONArray("uncertain").strings()).also { it.validate() }
        }
        require(trips.map { it.id }.distinct().size == trips.size) { "Duplicate swim IDs in backup." }
        return trips.sortedByDescending { it.date }
    }
    // Import adds missing swims and never silently overwrites a local edit.
    fun merge(local: List<Trip>, incoming: List<Trip>): List<Trip> =
        (local + incoming.filter { t -> local.none { it.id == t.id } }).sortedByDescending { it.date }
}

class JournalModel(application: Application) : AndroidViewModel(application) {
    private val file = AtomicFile(File(application.filesDir, "journal.json"))
    var trips by mutableStateOf<List<Trip>>(emptyList()); private set
    var loading by mutableStateOf(true); private set
    var busy by mutableStateOf(false); private set
    var loadError by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null)
    init {
        viewModelScope.launch {
            try {
                trips = withContext(Dispatchers.IO) {
                    if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists())
                        JournalCodec.decode(file.openRead().bufferedReader().use { it.readText() }) else emptyList()
                }
            } catch (_: Exception) { loadError = "Your journal could not be read. The original file has been kept. Restore a backup or contact support before logging more swims." }
            finally { loading = false }
        }
    }
    private suspend fun persist(next: List<Trip>) = withContext(Dispatchers.IO) {
        val stream = file.startWrite()
        try { stream.write(JournalCodec.encode(next).toByteArray()); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
    fun save(trip: Trip, done: () -> Unit) {
        if (busy || loading || loadError != null) return
        try { trip.validate() } catch (e: Exception) { message = e.message ?: "Check your swim details."; return }
        update((trips.filterNot { it.id == trip.id } + trip).sortedByDescending { it.date }, "Swim saved. Another little adventure!", done)
    }
    fun delete(trip: Trip, done: () -> Unit) {
        if (busy || loading || loadError != null) return
        update(trips.filterNot { it.id == trip.id }, "Swim deleted.", done)
    }
    private fun update(next: List<Trip>, success: String, done: () -> Unit) {
        busy = true
        viewModelScope.launch {
            try { persist(next); trips = next; message = success; done() }
            catch (_: Exception) { message = "Couldn't save. Your previous journal is safe. Please try again." }
            finally { busy = false }
        }
    }
    fun importBackup(read: () -> String) {
        if (busy || loading) return
        busy = true
        viewModelScope.launch {
            try {
                val incoming = withContext(Dispatchers.IO) { JournalCodec.decode(read()) }
                val next = JournalCodec.merge(trips, incoming)
                val added = next.size - trips.size
                // Retain unreadable original data before recovering from a valid backup.
                withContext(Dispatchers.IO) {
                    if (loadError != null && file.baseFile.exists())
                        file.baseFile.copyTo(File(file.baseFile.parentFile, "journal-unreadable-${System.currentTimeMillis()}.json"))
                }
                persist(next); trips = next; loadError = null
                message = "$added swims restored. Existing swims kept."
            } catch (_: Exception) { message = "Couldn't restore this backup. No swims were changed." }
            finally { busy = false }
        }
    }
    fun exportBackup(write: (String) -> Unit) {
        if (busy || loading || loadError != null) return
        val snapshot = trips
        busy = true
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { write(JournalCodec.encode(snapshot)) }; message = "Backup saved." }
            catch (_: Exception) { message = "Couldn't export. Your journal is still on this device." }
            finally { busy = false }
        }
    }
}
