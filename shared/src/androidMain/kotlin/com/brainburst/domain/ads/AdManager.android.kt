package com.brainburst.domain.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class AdManager(private val activity: Activity) {
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    
    // Interstitial ad for leaderboard (short ad, 5-15 seconds)
    private val interstitialAdUnitId = "ca-app-pub-2135414691513930/8388866066"
    
    // Rewarded ad for hints (longer ad, 30 seconds, higher revenue)
    private val rewardedAdUnitId = "ca-app-pub-2135414691513930/2454739457"
    actual fun preloadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            activity,
            interstitialAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d("AdManager", "✅ Interstitial ad loaded (for leaderboard)")
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e("AdManager", "❌ Failed to load interstitial ad: ${error.message}")
                }
            }
        )
    }
    
    actual fun preloadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            activity,
            rewardedAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d("AdManager", "✅ Rewarded ad loaded (for hints)")
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e("AdManager", "❌ Failed to load rewarded ad: ${error.message}")
                }
            }
        )
    }
    
    actual suspend fun showInterstitialAd(onAdClosed: () -> Unit) {
        suspendCancellableCoroutine { continuation ->
            val ad = interstitialAd
            
            if (ad == null) {
                Log.w("AdManager", "⚠️ No interstitial ad loaded, proceeding anyway")
                continuation.resume(Unit)
                onAdClosed()
                preloadInterstitialAd()
                return@suspendCancellableCoroutine
            }
            
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdManager", "📱 Interstitial ad dismissed")
                    interstitialAd = null
                    continuation.resume(Unit)
                    onAdClosed()
                    preloadInterstitialAd()
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e("AdManager", "❌ Failed to show interstitial: ${error.message}")
                    interstitialAd = null
                    continuation.resume(Unit)
                    onAdClosed()
                    preloadInterstitialAd()
                }
                
                override fun onAdShowedFullScreenContent() {
                    Log.d("AdManager", "📺 Showing interstitial ad")
                }
            }
            
            ad.show(activity)
        }
    }
    
    actual suspend fun showRewardedAd(onRewarded: () -> Unit) {
        suspendCancellableCoroutine { continuation ->
            val ad = rewardedAd
            
            if (ad == null) {
                Log.w("AdManager", "⚠️ No rewarded ad loaded, giving hint anyway")
                continuation.resume(Unit)
                onRewarded()
                preloadRewardedAd()
                return@suspendCancellableCoroutine
            }
            
            var rewardEarned = false
            
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdManager", "📱 Rewarded ad dismissed (reward earned: $rewardEarned)")
                    rewardedAd = null
                    continuation.resume(Unit)
                    
                    // Only give reward if user watched the full ad
                    if (rewardEarned) {
                        onRewarded()
                    }
                    
                    preloadRewardedAd()
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e("AdManager", "❌ Failed to show rewarded ad: ${error.message}")
                    rewardedAd = null
                    continuation.resume(Unit)
                    // Give reward anyway on error (good user experience)
                    onRewarded()
                    preloadRewardedAd()
                }
                
                override fun onAdShowedFullScreenContent() {
                    Log.d("AdManager", "📺 Showing rewarded ad (30 seconds)")
                }
            }
            
            ad.show(activity) { rewardItem ->
                // User earned the reward by watching the full ad
                rewardEarned = true
                Log.d("AdManager", "🎁 User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            }
        }
    }
}


