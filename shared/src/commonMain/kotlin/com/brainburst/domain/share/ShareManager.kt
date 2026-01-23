package com.brainburst.domain.share

/**
 * Platform-specific share manager for sharing app links
 */
expect class ShareManager {
    /**
     * Share the app with others using the platform's native share sheet
     */
    fun shareApp()
}
