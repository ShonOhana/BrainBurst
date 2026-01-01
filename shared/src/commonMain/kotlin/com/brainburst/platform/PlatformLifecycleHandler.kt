package com.brainburst.platform

import androidx.compose.runtime.Composable

/**
 * Platform-specific lifecycle handler for screen lifecycle events.
 * On Android: Observes Activity lifecycle and calls onStop when ON_PAUSE event occurs
 * (more reliable than ON_STOP when app is killed or goes to background).
 * On iOS: Calls onStop when the composable is disposed (equivalent to viewDidDisappear).
 */
@Composable
expect fun PlatformLifecycleHandler(onStop: () -> Unit)

