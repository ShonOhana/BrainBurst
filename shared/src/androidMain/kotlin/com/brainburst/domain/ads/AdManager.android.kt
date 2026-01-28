package com.brainburst.domain.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class AdManager(private val activity: Activity) {
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var bannerView: AdView? = null
    
    // Ad Unit IDs
    private val interstitialAdUnitId = "ca-app-pub-2135414691513930/8388866066"
    private val rewardedAdUnitId = "ca-app-pub-2135414691513930/2454739457"
    
    private val bannerAdUnitId = "ca-app-pub-2135414691513930/8506346134"
    private val nativeAdUnitId = "ca-app-pub-2135414691513930/6039964896"
    
    // Frequency capping for interstitials
    private var lastInterstitialTime = 0L
    private var gamesCompletedCount = 0
    private val MIN_INTERSTITIAL_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    private val GAMES_BETWEEN_INTERSTITIALS = 2 // Show ad after 2 daily puzzles
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
    
    // ============ BANNER ADS ============
    
    actual fun loadBanner(onBannerLoaded: (Any) -> Unit) {
        bannerView?.destroy()
        
        bannerView = AdView(activity).apply {
            adUnitId = bannerAdUnitId
            setAdSize(AdSize.BANNER)
            
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("AdManager", "✅ Banner ad loaded")
                    onBannerLoaded(this@apply)
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AdManager", "❌ Failed to load banner ad: ${error.message}")
                }
            }
            
            loadAd(AdRequest.Builder().build())
        }
    }
    
    actual fun destroyBanner() {
        bannerView?.destroy()
        bannerView = null
        Log.d("AdManager", "🗑️ Banner ad destroyed")
    }
    
    // ============ NATIVE ADS ============
    
    actual suspend fun loadNativeAd(): Any? = suspendCancellableCoroutine { continuation ->
        val adLoader = AdLoader.Builder(activity, nativeAdUnitId)
            .forNativeAd { nativeAd ->
                Log.d("AdManager", "✅ Native ad loaded")
                continuation.resume(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AdManager", "❌ Failed to load native ad: ${error.message}")
                    continuation.resume(null)
                }
            })
            .build()
        
        adLoader.loadAd(AdRequest.Builder().build())
    }
    
    // ============ FREQUENCY CAPPING ============
    
    actual fun shouldShowInterstitial(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAd = currentTime - lastInterstitialTime
        val hasWaitedLongEnough = timeSinceLastAd >= MIN_INTERSTITIAL_INTERVAL_MS
        val hasPlayedEnoughGames = gamesCompletedCount >= GAMES_BETWEEN_INTERSTITIALS
        
        val shouldShow = hasWaitedLongEnough && hasPlayedEnoughGames
        
        Log.d("AdManager", """
            🎯 Interstitial frequency check:
            - Time since last ad: ${timeSinceLastAd / 1000}s (min: ${MIN_INTERSTITIAL_INTERVAL_MS / 1000}s)
            - Games completed: $gamesCompletedCount (min: $GAMES_BETWEEN_INTERSTITIALS)
            - Should show: $shouldShow
        """.trimIndent())
        
        return shouldShow
    }
    
    actual fun recordInterstitialShown() {
        lastInterstitialTime = System.currentTimeMillis()
        gamesCompletedCount = 0 // Reset counter after showing ad
        Log.d("AdManager", "📊 Interstitial shown - counter reset")
    }
    
    /**
     * Call this when a game is completed to increment the counter
     * This is used for frequency capping
     */
    actual fun recordGameCompleted() {
        gamesCompletedCount++
        Log.d("AdManager", "🎮 Game completed - count: $gamesCompletedCount")
    }
}