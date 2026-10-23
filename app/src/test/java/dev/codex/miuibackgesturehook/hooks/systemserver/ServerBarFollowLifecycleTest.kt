package dev.codex.miuibackgesturehook.hooks.systemserver

import java.util.ArrayDeque
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.FutureTask
import java.util.concurrent.Callable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ServerBarFollowLifecycleTest {
    @Test fun reloadWaitsForBothObservedCommitAndNativeRestoreReturn() {
        val lifecycle = ServerBarFollowLifecycle()
        val controller = Any()
        val owner = Executors.newSingleThreadExecutor()
        val entered = CountDownLatch(1)
        val allowReturn = CountDownLatch(1)
        var commit: Runnable? = null
        try {
            lifecycle.begin(controller, owner) {
                commit = it
                entered.countDown()
                check(allowReturn.await(2, TimeUnit.SECONDS))
            }
            assertFalse(lifecycle.reserveReload())
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            val waiting = FutureTask(Callable { lifecycle.awaitReload(2000) })
            val caller = Thread(waiting, "bar-reload-test")
            caller.start()
            assertNotNull(commit).run()
            assertFalse(lifecycle.awaitReload(0))
            allowReturn.countDown()
            assertTrue(waiting.get(2, TimeUnit.SECONDS))
            assertNull(lifecycle.enterMutation(controller))
            lifecycle.cancelReloadReservation()
            lifecycle.begin(controller, Executor { it.run() }) { it.run() }
            assertNotNull(lifecycle.enterMutation(controller)).close()
        } finally {
            allowReturn.countDown()
            owner.shutdownNow()
        }
    }

    @Test fun timeoutOrFailureNeverCountsAsRestoration() {
        val lifecycle = ServerBarFollowLifecycle()
        val controller = Any()
        var commit: Runnable? = null
        lifecycle.begin(controller, Executor { it.run() }) { commit = it }
        assertFalse(lifecycle.reserveReload())
        assertFalse(lifecycle.awaitReload(1))
        assertFailsWith<IllegalStateException> { lifecycle.retiredControllers() }
        assertNotNull(commit).run()
        assertTrue(lifecycle.awaitReload(0))

        val failed = ServerBarFollowLifecycle()
        failed.begin(Any(), Executor { it.run() }) { error("native restore rejected") }
        assertFalse(failed.reserveReload())
        assertFalse(failed.awaitReload(1000))
    }

    @Test fun synchronousWritesDrainBeforeRestoreAndBypassIsOwnerLocal() {
        val lifecycle = ServerBarFollowLifecycle()
        val owner = Any()
        val other = Any()
        val queue = ArrayDeque<Runnable>()
        var commit: Runnable? = null
        lifecycle.begin(other, Executor { queue.add(it) }) { }
        lifecycle.retire(other)
        lifecycle.begin(owner, Executor { queue.add(it) }) {
            assertNotNull(lifecycle.enterMutation(owner)).close()
            assertNull(lifecycle.enterMutation(other))
            assertNull(lifecycle.enter(Any(), owner))
            commit = it
        }
        val write = assertNotNull(lifecycle.enterMutation(owner))
        lifecycle.retire(owner)
        assertNull(lifecycle.enterMutation(owner))
        assertEquals(1, queue.size)
        write.close()
        assertEquals(2, queue.size)
        queue.remove().run()
        queue.remove().run()
        assertNull(lifecycle.enterMutation(owner))
        assertNotNull(commit).run()
        assertNull(lifecycle.enterMutation(owner))
    }

    @Test fun earlyCommitDoesNotHideBookkeepingFailure() {
        val lifecycle = ServerBarFollowLifecycle()
        val owner = Any()
        var fail = true
        lifecycle.begin(owner, Executor { it.run() }) {
            it.run()
            assertFailsWith<IllegalStateException> { lifecycle.retiredControllers() }
            if (fail) error("native bookkeeping failed after commit")
        }
        lifecycle.retire(owner)
        assertNotNull(lifecycle.failure(owner))
        assertFailsWith<IllegalStateException> { lifecycle.retiredControllers() }
        fail = false
        lifecycle.retire(owner)
        assertEquals(owner, lifecycle.retiredControllers().single())
        assertNull(lifecycle.failure(owner))
    }

    @Test fun callbacksCreatedWhileRetiringCannotEnterNextGeneration() {
        val lifecycle = ServerBarFollowLifecycle()
        val owner = Any()
        var commit: Runnable? = null
        lifecycle.begin(owner, Executor { it.run() }) { commit = it }
        val active = Any()
        lifecycle.created(active, owner)
        val permit = assertNotNull(lifecycle.enter(active, owner))
        lifecycle.retire(owner)
        val queuedByOldCallback = Any()
        lifecycle.created(queuedByOldCallback, owner)
        permit.close()
        assertNotNull(commit).run()
        lifecycle.begin(owner, Executor { it.run() }) { it.run() }
        assertNull(lifecycle.enter(queuedByOldCallback, owner))
        val current = Any()
        lifecycle.created(current, owner)
        assertNotNull(lifecycle.enter(current, owner)).close()
    }

    @Test fun retirementWaitsForEnteredCallbackAndSurfaceCommit() {
        val lifecycle = ServerBarFollowLifecycle()
        val owner = Any()
        val callback = Any()
        val queued = ArrayDeque<Runnable>()
        var committed: Runnable? = null
        lifecycle.begin(owner, Executor { queued.add(it) }) { committed = it }
        lifecycle.created(callback, owner)
        val entered = assertNotNull(lifecycle.enter(callback, owner))
        assertFalse(lifecycle.reserveReload())
        assertNull(lifecycle.enter(callback, owner))
        assertTrue(queued.isEmpty())
        entered.close()
        entered.close()
        assertEquals(1, queued.size)
        queued.remove().run()
        assertFalse(lifecycle.reserveReload())
        assertNotNull(committed).run()
        assertTrue(lifecycle.reserveReload())
    }

    @Test fun listenerCannotBorrowReplacementLifetime() {
        val lifecycle = ServerBarFollowLifecycle()
        val owner = Any()
        val old = Any()
        lifecycle.created(old, owner)
        lifecycle.begin(owner, Executor { it.run() }) { it.run() }
        assertNull(lifecycle.enter(old, owner))
        val first = Any()
        lifecycle.created(first, owner)
        assertNotNull(lifecycle.enter(first, owner)).close()
        lifecycle.retire(owner)
        lifecycle.begin(owner, Executor { it.run() }) { it.run() }
        assertNull(lifecycle.enter(first, owner))
        val second = Any()
        lifecycle.created(second, owner)
        assertNotNull(lifecycle.enter(second, owner)).close()
    }

    @Test fun failedRestoreCannotBeCompletedByItsLateCommit() {
        val lifecycle = ServerBarFollowLifecycle()
        val owner = Any()
        val commits = mutableListOf<Runnable>()
        lifecycle.begin(owner, Executor { it.run() }) {
            commits.add(it)
            if (commits.size == 1) error("apply failed")
        }
        lifecycle.retire(owner)
        assertNotNull(lifecycle.failure(owner))
        lifecycle.retire(owner)
        commits[0].run()
        assertFailsWith<IllegalStateException> { lifecycle.retiredControllers() }
        commits[1].run()
        assertEquals(owner, lifecycle.retiredControllers().single())
    }

    @Test fun reloadRetainsDenialIncludingReplacedNativeController() {
        val oldRuntime = ServerBarFollowLifecycle()
        val owner = Any()
        val queuedStart = Any()
        oldRuntime.begin(owner, Executor { it.run() }) { it.run() }
        oldRuntime.created(queuedStart, owner)
        assertTrue(oldRuntime.reserveReload())
        val replacement = ServerBarFollowLifecycle()
        replacement.restoreRetiredControllers(oldRuntime.retiredControllers())
        replacement.restoreRetiredCallbacks(oldRuntime.retiredCallbacks())
        assertNull(replacement.enter(queuedStart, owner))
        assertNull(replacement.enter(queuedStart, Any()))
        assertNotNull(replacement.enter(Any(), Any())).close()
    }
}
