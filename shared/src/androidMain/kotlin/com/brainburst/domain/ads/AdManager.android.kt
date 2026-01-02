package com.brainburst.domain.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class AdManager(private val activity: Activity) {
    private var interstitialAd: InterstitialAd? = null
    private val adUnitId = "ca-app-pub-2135414691513930/8388866066" // ✅ זה Ad Unit ID הנכון
    actual fun preloadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            activity,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d("AdManager", "Interstitial ad loaded")
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e("AdManager", "Failed to load interstitial ad: ${error.message}")
                }
            }
        )
    }
    
    actual suspend fun showInterstitialAd(onAdClosed: () -> Unit) {
        suspendCancellableCoroutine { continuation ->
            val ad = interstitialAd
            
            if (ad == null) {
                // If no ad is loaded, just call the callback immediately
                continuation.resume(Unit)
                onAdClosed()
                // Preload for next time
                preloadInterstitialAd()
                return@suspendCancellableCoroutine
            }
            
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    continuation.resume(Unit)
                    onAdClosed()
                    // Preload for next time
                    preloadInterstitialAd()
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    continuation.resume(Unit)
                    onAdClosed()
                    // Preload for next time
                    preloadInterstitialAd()
                }
                
                override fun onAdShowedFullScreenContent() {
                    // Ad is showing
                }
            }
            
            ad.show(activity)
        }
    }
}

