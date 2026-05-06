package com.dynodevv.relay.ui.providers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dynodevv.relay.domain.model.Provider

@Composable
fun ProviderIcon(
    provider: Provider,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val bgColor = remember(provider.name) {
        val hash = provider.name.hashCode()
        Color(
            red = ((hash shr 16) and 0xFF) / 255f * 0.5f + 0.25f,
            green = ((hash shr 8) and 0xFF) / 255f * 0.5f + 0.25f,
            blue = (hash and 0xFF) / 255f * 0.5f + 0.25f
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .background(bgColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = provider.name.take(2).uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
