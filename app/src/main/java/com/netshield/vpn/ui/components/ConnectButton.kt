package com.netshield.vpn.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netshield.vpn.ui.theme.NetShieldColors

@Composable
fun ConnectButton(
    connected: Boolean,
    connecting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringAlpha by animateFloatAsState(
        targetValue = if (connected) 1f else 0.35f,
        animationSpec = tween(400),
        label = "ringAlpha"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(220.dp)
            .border(3.dp, NetShieldColors.Accent.copy(alpha = ringAlpha), CircleShape)
            .padding()
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = false, radius = 110.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .background(NetShieldColors.Surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PowerSettingsNew,
                contentDescription = if (connected) "قطع اتصال" else "اتصال",
                tint = if (connected) NetShieldColors.Accent else NetShieldColors.TextSecondary,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

private fun Modifier.padding() = this
