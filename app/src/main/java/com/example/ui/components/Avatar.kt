package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealPrimary
import kotlin.math.abs

@Composable
fun UserAvatar(
    name: String,
    photoUrl: String?,
    size: Dp = 48.dp,
    isOnline: Boolean? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        if (!photoUrl.isNullOrBlank() && (photoUrl.startsWith("http") || photoUrl.startsWith("content"))) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Distinct cheerful color based on name hash
            val palette = listOf(
                Color(0xFF0F766E), Color(0xFF0284C7), Color(0xFF7C3AED),
                Color(0xFFDB2777), Color(0xFFEA580C), Color(0xFF059669)
            )
            val colorIndex = abs(name.hashCode()) % palette.size
            val bgColor = palette[colorIndex]
            val initial = name.trim().firstOrNull()?.toString()?.uppercase() ?: "আ"

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42).sp
                )
            }
        }

        if (isOnline != null) {
            val dotSize = (size * 0.28f).coerceIn(10.dp, 16.dp)
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) OnlineGreen else Color(0xFF94A3B8))
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}
