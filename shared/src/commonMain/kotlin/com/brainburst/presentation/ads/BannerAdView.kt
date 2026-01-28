package com.brainburst.presentation.ads

import androidx.compose.runtime.Composable
import com.brainburst.domain.ads.AdManager

@Composable
expect fun BannerAdView(adManager: AdManager)
