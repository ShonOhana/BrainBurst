package com.brainburst.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * iOS implementation: Calls onStop when the composable is disposed.
 * In Compose Multiplatform on iOS, DisposableEffect.onDispose is called
 * when the composable is removed from composition, which happens when
 * the view controller's viewDidDisappear occurs.
 */
@Composable
actual fun PlatformLifecycleHandler(onStop: () -> Unit) {
    DisposableEffect(Unit) {
        onDispose {
            onStop()
        }
    }
}

