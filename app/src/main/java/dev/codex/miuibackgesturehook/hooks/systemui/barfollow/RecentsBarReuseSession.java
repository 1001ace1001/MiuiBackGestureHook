package dev.codex.miuibackgesturehook.hooks.systemui.barfollow;

/** Caller serializes callback delivery and Shell handoff; identities are never time based. */
final class RecentsBarReuseSession {
    final Object owner, controller, transition, info, task;
    Object callback, original, endpoint;
    final int taskId;
    private boolean homeTarget;
    private boolean closed;

    RecentsBarReuseSession(Object owner, Object controller, Object transition, Object info,
            Object callback, Object original, Object endpoint, Object task, int taskId) {
        this.owner = owner; this.controller = controller; this.transition = transition;
        this.info = info; this.callback = callback; this.original = original; this.endpoint = endpoint;
        this.task = task; this.taskId = taskId;
    }

    void delivered(Object source, boolean authenticated, int state) {
        if (closed || source != callback || !authenticated) return;
        // Snapshot/blur delivery is not a new transition or a new alpha target.
        // Native state 3 updates gesture bookkeeping only. It can arrive after
        // state 1 and must not erase the still-active Home alpha target.
        if (state == 3 || state == 6 || state == 14) return;
        homeTarget = state == 1;
    }

    boolean adopt(Object sourceOwner, Object source, Object sourceOriginal, Object sourceEndpoint,
            boolean authenticated, int state) {
        if (closed || !authenticated || sourceOwner != owner || source == null
                || sourceOriginal == null || sourceEndpoint == null) return false;
        // Start acknowledgement binds the actual retained Launcher callback. Never
        // adopt a terminal state or replace it with Shell's newest publication.
        if (callback == null) {
            if (state != 12) return false;
            callback = source; original = sourceOriginal; endpoint = sourceEndpoint;
        }
        return callback == source && endpoint == sourceEndpoint;
    }

    boolean consume(Object currentController, Object currentTransition, Object currentInfo,
            Object currentCallback, Object currentTask, boolean finishToHome, boolean live) {
        if (closed || !homeTarget || !live || finishToHome
                || currentController != controller || currentTransition != transition
                || currentInfo != info || currentCallback != callback || currentTask != task) return false;
        homeTarget = false;
        return true;
    }

    void close() { closed = true; homeTarget = false; }

}
