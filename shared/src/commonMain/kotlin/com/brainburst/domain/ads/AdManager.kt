package com.brainburst.domain.ads

/**
 * Platform-specific ad manager
 * Handles showing interstitial ads before game start and leaderboard
 * 
 * Note: Platform-specific constructors are allowed in expect/actual pattern
 */
expect class AdManager {
    /**
     * Shows an interstitial ad and calls onAdClosed when done
     * @param onAdClosed Callback when ad is closed (whether watched or dismissed)
     */
    suspend fun showInterstitialAd(onAdClosed: () -> Unit)
    
    /**
     * Preloads an interstitial ad for better performance
     */
    fun preloadInterstitialAd()
}

