package com.brainburst.platform

import androidx.compose.runtime.Composable

/**
 * Platform-specific lifecycle handler for app lifecycle events.
 * On Android: Observes Activity lifecycle and calls onPause/onResume when app goes to background/foreground.
 * On iOS: Calls onPause/onResume when app lifecycle changes.
 */
@Composable
expect fun PlatformLifecycleHandler(
    onPause: () -> Unit,
    onResume: () -> Unit
)

