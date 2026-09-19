package org.dergigi.fishyfishy

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.util.Locale

internal data class SpeechRequest(val id: String, val words: String, val language: String, val playing: Boolean = false)

/** Unique IDs prevent late completion callbacks from clearing a newer request. */
internal class SpeechRequests {
    var active by mutableStateOf<SpeechRequest?>(null); private set
    private var serial = 0L
    fun begin(words: String, language: String): SpeechRequest? {
        if (active?.let { it.words == words && it.language == language } == true) return null
        return SpeechRequest((++serial).toString(), words, language).also { active = it }
    }
    fun started(id: String) { if (active?.id == id) active = active?.copy(playing = true) }
    fun finish(id: String): Boolean {
        if (active?.id != id) return false
        active = null
        return true
    }
    fun clear() { active = null }
}

internal class Speaker(private val context: Context, private val scope: CoroutineScope) {
    val requests = SpeechRequests()
    private val handler = Handler(Looper.getMainLooper())
    private val initialized = CompletableDeferred<Boolean>()
    private var closed = false
    private var job: Job? = null
    private val engine = TextToSpeech(context) { status -> initialized.complete(status == TextToSpeech.SUCCESS) }.apply {
        setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) { handler.post { if (!closed) requests.started(utteranceId) } }
            override fun onDone(utteranceId: String) { handler.post { requests.finish(utteranceId) } }
            @Deprecated("Android compatibility callback")
            override fun onError(utteranceId: String) = failed(utteranceId)
            override fun onError(utteranceId: String, errorCode: Int) = failed(utteranceId)
            override fun onStop(utteranceId: String, interrupted: Boolean) { handler.post { requests.finish(utteranceId) } }
            private fun failed(id: String) { handler.post { fail(id, "Couldn't play the voice. Please try again.") } }
        })
    }
    fun speak(words: String, language: String) {
        if (closed) return
        val request = requests.begin(words, language) ?: return
        job?.cancel()
        job = scope.launch {
            // Give Compose a frame to show feedback before any engine/binder work.
            withFrameNanos { }
            try {
                engine.stop()
                if (!withTimeout(8000) { initialized.await() }) {
                    fail(request.id, "Android's text-to-speech engine couldn't start. Check its settings and try again.")
                    return@launch
                }
                val locale = when (language) { "pt" -> Locale.forLanguageTag("pt-PT"); "de" -> Locale.GERMAN; else -> Locale.UK }
                val voice = withContext(Dispatchers.IO) {
                    engine.voices?.filter { !it.isNetworkConnectionRequired && it.locale.language == locale.language }
                        ?.sortedByDescending { it.locale.country == locale.country }?.firstOrNull()
                }
                if (voice == null) {
                    fail(request.id, "Install an offline ${locale.getDisplayLanguage(Locale.ENGLISH)} voice in Android's text-to-speech settings to listen.")
                    return@launch
                }
                if (requests.active?.id != request.id) return@launch
                if (engine.setVoice(voice) == TextToSpeech.ERROR || engine.setSpeechRate(0.85f) == TextToSpeech.ERROR ||
                    engine.speak(words, TextToSpeech.QUEUE_FLUSH, null, request.id) == TextToSpeech.ERROR) {
                    fail(request.id, "Couldn't play the voice. Please try again.")
                    return@launch
                }
                delay(12000)
                if (requests.active?.let { it.id == request.id && !it.playing } == true) {
                    fail(request.id, "The voice took too long to start. Please try again."); engine.stop()
                }
                delay(60000)
                if (requests.finish(request.id)) engine.stop()
            } catch (e: TimeoutCancellationException) {
                fail(request.id, "The voice took too long to start. Please try again.")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { fail(request.id, "Couldn't play the voice. Please try again.") }
        }
    }
    private fun fail(id: String, message: String) {
        if (!closed && requests.finish(id)) Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    fun stop() { job?.cancel(); requests.clear(); engine.stop() }
    fun close() { closed = true; stop(); engine.shutdown() }
}

@Composable internal fun rememberSpeaker(): Speaker {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val speaker = remember(context, scope) { Speaker(context, scope) }
    DisposableEffect(speaker) { onDispose { speaker.close() } }
    return speaker
}

@Composable internal fun SpeakButton(speaker: Speaker, words: String, language: String, description: String) {
    val active = speaker.requests.active?.takeIf { it.words == words && it.language == language }
    val label = when { active == null -> "Listen"; active.playing -> "Playing…"; else -> "Starting…" }
    IconButton(
        onClick = { speaker.speak(words, language) }, enabled = active == null,
        modifier = Modifier.semantics {
            contentDescription = description
            stateDescription = label
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        when {
            active == null -> Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = MaterialTheme.colorScheme.primary)
            !active.playing -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else -> SpeakingIcon()
        }
    }
}

@Composable private fun SpeakingIcon() {
    val animation = rememberInfiniteTransition(label = "Speaking")
    Row(Modifier.size(24.dp), horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val height by animation.animateFloat(
                initialValue = 6f, targetValue = 22f,
                animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse, StartOffset(index * 150)),
                label = "Sound bar $index",
            )
            Box(Modifier.width(4.dp).height(height.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
        }
    }
}
