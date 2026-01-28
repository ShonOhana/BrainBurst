package com.brainburst.presentation.ads

import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.brainburst.domain.ads.AdManager
import com.google.android.gms.ads.AdView

@Composable
actual fun BannerAdView(adManager: AdManager) {
    val context = LocalContext.current
    val adView = remember { mutableListOf<View>() }
    
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx ->
            android.widget.FrameLayout(ctx).apply {
                adManager.loadBanner { bannerView ->
                    removeAllViews()
                    val view = bannerView as AdView
                    addView(view)
                    adView.clear()
                    adView.add(view)
                }
            }
        }
    )
    
    DisposableEffect(Unit) {
        onDispose {
            adManager.destroyBanner()
        }
    }
}
