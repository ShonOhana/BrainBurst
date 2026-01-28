package com.brainburst.domain.ads

/**
 * Platform-specific ad manager
 * Handles all ad types: interstitial, rewarded, banner, and native ads
 * 
 * Note: Platform-specific constructors are allowed in expect/actual pattern
 */
expect class AdManager {
    /**
     * Shows an interstitial ad and calls onAdClosed when done
     * Used for: After every 3 completed games (with 5-minute cooldown)
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
    
    /**
     * Loads a banner ad view
     * Used for: Bottom of game screens (passive income)
     * @param onBannerLoaded Callback with the platform-specific banner view
     */
    fun loadBanner(onBannerLoaded: (Any) -> Unit)
    
    /**
     * Destroys the banner ad and cleans up resources
     */
    fun destroyBanner()
    
    /**
     * Loads a native ad for integration into UI
     * Used for: Leaderboard list (every 5 entries)
     * @return Platform-specific native ad object, or null if failed
     */
    suspend fun loadNativeAd(): Any?
    
    /**
     * Checks if an interstitial ad should be shown based on frequency capping
     * Rules: Max 1 per 5 minutes, only after every 3 games
     * @return true if ad should be shown, false otherwise
     */
    fun shouldShowInterstitial(): Boolean
    
    /**
     * Records that an interstitial ad was shown (for frequency capping)
     */
    fun recordInterstitialShown()
}

