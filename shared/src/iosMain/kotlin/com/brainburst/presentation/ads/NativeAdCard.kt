package com.brainburst.presentation.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.brainburst.domain.ads.AdManager

@Composable
actual fun NativeAdCard(adManager: AdManager) {
    // iOS native ads not implemented yet - show empty box
    Box {}
}
