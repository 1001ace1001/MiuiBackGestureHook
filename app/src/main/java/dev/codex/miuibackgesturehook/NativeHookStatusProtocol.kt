package dev.codex.miuibackgesturehook

/** Shared wire names for the UI, SystemUI, and native-hook status challenge. */
object NativeHookStatusProtocol {
    const val PACKAGE_NAME = "dev.codex.miuibackgesturehook"
    const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    const val ACTION_QUERY =
        "dev.codex.miuibackgesturehook.action.RUNTIME_STATUS_QUERY"
    const val ACTION_REPLY =
        "dev.codex.miuibackgesturehook.action.RUNTIME_STATUS_REPLY"
    const val ACTION_SYSTEMUI_STATE =
        "dev.codex.miuibackgesturehook.action.SYSTEMUI_INPUT_ARBITER_STATE"

    const val EXTRA_NONCE = "status_nonce"
    const val EXTRA_QUERY = "status_query"
    const val EXTRA_NATIVE_RESPONSE = "status_native_response"
    const val EXTRA_LEGACY_MODE = "status_legacy_mode"
    const val EXTRA_LEGACY_READY = "status_legacy_ready"
    const val EXTRA_NATIVE_READY = "status_native_ready"
    const val EXTRA_NATIVE_PROFILE_RESOLVED = "status_native_profile_resolved"
    const val EXTRA_NATIVE_PROFILE_DYNAMIC = "status_native_profile_dynamic"
    const val EXTRA_NATIVE_PROFILE_ENTRY_OFFSET = "status_native_profile_entry_offset"
    const val EXTRA_NATIVE_SIDE_OFFSET = "status_native_side_offset"
    const val EXTRA_NATIVE_RUNTIME_PROFILE_STAGE = "status_native_runtime_profile_stage"
    const val EXTRA_NATIVE_DART_RESOLVER_STAGE = "status_native_dart_resolver_stage"
    const val EXTRA_NATIVE_DART_DRAWER_CANDIDATES = "status_native_dart_drawer_candidates"
    const val EXTRA_NATIVE_DART_TRANSITION_CANDIDATES =
        "status_native_dart_transition_candidates"
    const val EXTRA_NATIVE_DART_OVERVIEW_ENTER_CANDIDATES =
        "status_native_dart_overview_enter_candidates"
    const val EXTRA_NATIVE_DART_OVERVIEW_EXIT_CANDIDATES =
        "status_native_dart_overview_exit_candidates"
    const val EXTRA_NATIVE_DART_EDITING_CANDIDATES =
        "status_native_dart_editing_candidates"
    const val EXTRA_NATIVE_DRAWER_STATE_READY = "status_native_drawer_state_ready"
    const val EXTRA_NATIVE_OVERVIEW_STATE_READY = "status_native_overview_state_ready"
    const val EXTRA_NATIVE_EDITING_STATE_READY = "status_native_editing_state_ready"
    const val EXTRA_NATIVE_BUSINESS_STATE = "status_native_business_state"
    const val EXTRA_NATIVE_BRIDGE_STATE = "status_native_bridge_state"
    const val EXTRA_NATIVE_RECEIVER_STATE = "status_native_receiver_state"
    const val EXTRA_SYSTEMUI_READY = "status_systemui_ready"
    const val EXTRA_SYSTEMUI_GENERATION = "status_systemui_generation"
    const val EXTRA_SYSTEMUI_MONITORS = "status_systemui_monitors"
    const val EXTRA_REASON = "status_reason"
}
