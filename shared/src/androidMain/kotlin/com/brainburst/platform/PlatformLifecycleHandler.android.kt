package com.brainburst.platform

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.koin.compose.koinInject

/**
 * Android implementation: Observes Activity lifecycle and calls onStop
 * when the Activity's ON_PAUSE event occurs (more reliable than ON_STOP
 * when the app is killed or goes to background).
 */
@Composable
actual fun PlatformLifecycleHandler(onStop: () -> Unit) {
    // Get ComponentActivity from Koin
    val activity: ComponentActivity = koinInject()
    
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            // Use ON_PAUSE instead of ON_STOP because it's more reliably called
            // when the app goes to background or is being killed
            if (event == Lifecycle.Event.ON_PAUSE) {
                onStop()
            }
        }
        
        activity.lifecycle.addObserver(observer)
        
        onDispose {
            activity.lifecycle.removeObserver(observer)
        }
    }
}

