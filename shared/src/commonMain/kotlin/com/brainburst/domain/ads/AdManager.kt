package com.brainburst.domain.ads

/**
 * Platform-specific ad manager
 * Handles showing interstitial ads (leaderboard) and rewarded ads (hints)
 * 
 * Note: Platform-specific constructors are allowed in expect/actual pattern
 */
expect class AdManager {
    /**
     * Shows an interstitial ad and calls onAdClosed when done
     * Used for: Leaderboard access
     * @param onAdClosed Callback when ad is closed (whether watched or dismissed)
     */
    suspend fun showInterstitialAd(onAdClosed: () -> Unit)
    
    /**
     * Shows a rewarded ad and calls onRewarded when user earns the reward
     * Used for: Hints in game
     * @param onRewarded Callback when user completes watching the ad and earns reward
     */
    suspend fun showRewardedAd(onRewarded: () -> Unit)
    
    /**
     * Preloads an interstitial ad for better performance
     */
    fun preloadInterstitialAd()
    
    /**
     * Preloads a rewarded ad for better performance
     */
    fun preloadRewardedAd()
}

