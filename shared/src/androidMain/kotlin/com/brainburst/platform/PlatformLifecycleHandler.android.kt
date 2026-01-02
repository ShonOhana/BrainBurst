package com.brainburst.platform

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.koin.compose.koinInject

/**
 * Android implementation: Observes Activity lifecycle and calls onPause/onResume
 * when the Activity's lifecycle events occur.
 */
@Composable
actual fun PlatformLifecycleHandler(
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    // Get ComponentActivity from Koin
    val activity: ComponentActivity = koinInject()
    
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // App is going to background
                    onPause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    // App is coming to foreground
                    onResume()
                }
                else -> {}
            }
        }
        
        activity.lifecycle.addObserver(observer)
        
        onDispose {
            activity.lifecycle.removeObserver(observer)
        }
    }
}

