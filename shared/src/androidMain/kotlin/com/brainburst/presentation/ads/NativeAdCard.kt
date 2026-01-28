package com.brainburst.presentation.ads

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brainburst.domain.ads.AdManager
import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.launch

@Composable
actual fun NativeAdCard(adManager: AdManager) {
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            val ad = adManager.loadNativeAd()
            nativeAd = ad as? NativeAd
        }
    }
    
    nativeAd?.let { ad ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                ad.icon?.let { icon ->
                    val drawable = icon.drawable
                    if (drawable is BitmapDrawable) {
                        Image(
                            bitmap = drawable.bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
                
                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ad.headline ?: "",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ad",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    ad.body?.let { body ->
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray,
                            maxLines = 2
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Call to action button
                ad.callToAction?.let { cta ->
                    Button(
                        onClick = { /* Click handled by AdMob */ },
                        modifier = Modifier.height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = cta,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
