package org.dergigi.fishyfishy

import org.junit.Assert.*
import org.junit.Test

class SpeechRequestsTest {
    @Test fun repeatedTapsDuringStartupAndPlaybackDoNotRestartTheName() {
        val state = SpeechRequests()
        val first = state.begin("Salema", "pt")!!
        repeat(5) { assertNull(state.begin("Salema", "pt")) }
        assertFalse(state.active!!.playing)
        state.started(first.id)
        assertTrue(state.active!!.playing)
        repeat(5) { assertNull(state.begin("Salema", "pt")) }
        assertTrue(state.finish(first.id))
        assertNotEquals(first.id, state.begin("Salema", "pt")!!.id)
    }

    @Test fun oldCallbacksCannotClearOrStartTheNewLanguage() {
        val state = SpeechRequests()
        val old = state.begin("Salema", "en")!!
        val current = state.begin("Salema", "pt")!!
        state.started(old.id)
        assertFalse(state.active!!.playing)
        assertFalse(state.finish(old.id))
        assertEquals(current, state.active)
    }

    @Test fun failureOrTimeoutAllowsRetryWithoutReusingAnUtteranceId() {
        val state = SpeechRequests()
        val failed = state.begin("Boga", "pt")!!
        assertTrue(state.finish(failed.id))
        val retry = state.begin("Boga", "pt")!!
        assertFalse(state.finish(failed.id))
        assertEquals(retry, state.active)
    }

    @Test fun leavingScreenClearsFeedbackAndIgnoresDelayedCallbacks() {
        val state = SpeechRequests()
        val old = state.begin("Mero", "pt")!!
        state.clear()
        state.started(old.id)
        assertNull(state.active)
        assertFalse(state.finish(old.id))
        assertNotNull(state.begin("Mero", "pt"))
    }
}
