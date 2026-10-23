-keep class dev.codex.miuibackgesturehook.MiuiBackGestureHook {
    public <init>();
}

# AndroidX Window probes these optional OEM-provided APIs at runtime. They are absent from the
# application classpath by design and must not make release shrinking fail.
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**

# These names describe interfaces loaded from SystemUI's ClassLoader. Their
# module-local copies must retain their ABI names too: R8 otherwise rewrites
# external reflection strings (for example StateFlow -> an obfuscated name).
# Keep only the shared reflection boundary; unused classes/members may shrink.
-keepnames class androidx.compose.runtime.CompositionContext { *; }
-keepnames class androidx.compose.runtime.internal.ComposableLambdaKt { *; }
-keepnames class androidx.compose.ui.platform.ComposeView { *; }
-keepnames class androidx.compose.ui.platform.WindowRecomposer_androidKt { *; }
-keepnames class androidx.lifecycle.Lifecycle$State { *; }
-keepnames class androidx.lifecycle.LifecycleOwner { *; }
-keepnames class androidx.lifecycle.LifecycleOwnerKt { *; }
-keepnames class androidx.lifecycle.RepeatOnLifecycleKt { *; }
-keepnames class androidx.lifecycle.ViewTreeLifecycleOwner { *; }
-keepnames class androidx.savedstate.ViewTreeSavedStateRegistryOwner { *; }
-keepnames class kotlin.LazyKt__LazyJVMKt { *; }
-keepnames class kotlin.Unit { *; }
-keepnames class kotlin.coroutines.Continuation { *; }
-keepnames class kotlin.coroutines.CoroutineContext { *; }
-keepnames class kotlin.coroutines.CoroutineContext$Key { *; }
-keepnames class kotlin.coroutines.intrinsics.CoroutineSingletons { *; }
-keepnames class kotlin.jvm.functions.Function0 { *; }
-keepnames class kotlin.jvm.functions.Function1 { *; }
-keepnames class kotlin.jvm.functions.Function2 { *; }
-keepnames class kotlin.jvm.functions.Function3 { *; }
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker { *; }
-keepnames class kotlinx.coroutines.BuildersKt { *; }
-keepnames class kotlinx.coroutines.CoroutineDispatcher { *; }
-keepnames class kotlinx.coroutines.CoroutineScope { *; }
-keepnames class kotlinx.coroutines.CoroutineStart { *; }
-keepnames class kotlinx.coroutines.DisposableHandle { *; }
-keepnames class kotlinx.coroutines.Job { *; }
-keepnames class kotlinx.coroutines.Job$Key { *; }
-keepnames class kotlinx.coroutines.SupervisorKt { *; }
-keepnames class kotlinx.coroutines.flow.Flow { *; }
-keepnames class kotlinx.coroutines.flow.StateFlow { *; }

-repackageclasses
-allowaccessmodification
-overloadaggressively
