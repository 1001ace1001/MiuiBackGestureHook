package dev.codex.miuibackgesturehook.hooks.systemui.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LauncherConnectionSessionTest {
    @Test fun connectedBaselineCanAdvertiseWithoutRestartingMonitor() {
        val state = LauncherConnectionSession()
        val binder = Any()
        val receipt = assertNotNull(state.observe(binder, 7))
        assertTrue(state.accepts(receipt, binder, 7))
        assertNull(state.observe(binder, 7))
        assertTrue(state.accepts(receipt, binder, 7))
    }

    @Test fun disconnectInvalidatesQueuedAdvertisementEvenIfSameBinderReconnects() {
        val state = LauncherConnectionSession()
        val binder = Any()
        val old = assertNotNull(state.observe(binder, 7))
        assertNull(state.observe(null, 7))
        assertFalse(state.accepts(old, binder, 7))
        val replacement = assertNotNull(state.observe(binder, 7))
        assertFalse(state.accepts(old, binder, 7))
        assertTrue(state.accepts(replacement, binder, 7))
    }

    @Test fun silentlyReplacedBinderCannotReceiveOldQueuedAdvertisement() {
        val state = LauncherConnectionSession()
        val first = Any()
        val second = Any()
        val old = assertNotNull(state.observe(first, 7))
        assertFalse(state.accepts(old, second, 7))
        val current = assertNotNull(state.observe(second, 7))
        assertFalse(state.accepts(old, first, 7))
        assertTrue(state.accepts(current, second, 7))
    }

    @Test fun generationChangeRejectsAnOldConnectionReceipt() {
        val state = LauncherConnectionSession()
        val binder = Any()
        val old = assertNotNull(state.observe(binder, 7))
        assertFalse(state.accepts(old, binder, 8))
        val current = assertNotNull(state.observe(binder, 8))
        assertFalse(state.accepts(old, binder, 7))
        assertTrue(state.accepts(current, binder, 8))
        assertNull(state.observe(binder, 0))
        assertFalse(state.accepts(current, binder, 8))
    }

    @Test fun hotReloadClosesEveryQueuedReceipt() {
        val state = LauncherConnectionSession()
        val binder = Any()
        val old = assertNotNull(state.observe(binder, 7))
        state.close()
        assertFalse(state.accepts(old, binder, 7))
        assertNull(state.observe(binder, 7))
    }
}
