package com.brainburst.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS doesn't have a hardware back button, so this is a no-op
    // iOS navigation is typically handled through navigation controllers or swipe gestures
}


