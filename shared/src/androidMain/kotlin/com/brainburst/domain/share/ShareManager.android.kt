package com.brainburst.domain.share

import android.content.Context
import android.content.Intent

actual class ShareManager(
    private val context: Context
) {
    actual fun shareApp() {
        val shareMessage = """
            Check out BrainBurst - Daily brain puzzles!
            
            Download it now:
            https://play.google.com/store/apps/details?id=com.brainburst.android
        """.trimIndent()
        
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            type = "text/plain"
        }
        
        val shareIntent = Intent.createChooser(sendIntent, "Share BrainBurst")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        context.startActivity(shareIntent)
    }
}
