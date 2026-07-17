package com.netshield.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netshield.vpn.ui.components.ConnectButton
import com.netshield.vpn.ui.theme.NetShieldColors

@Composable
fun StatusScreen(
    onOpenLocations: () -> Unit,
    onOpenSubscription: () -> Unit,
    viewModel: StatusViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NetShieldColors.Background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NetShield", style = MaterialTheme.typography.headlineLarge)
            IconButton(onClick = onOpenSubscription) {
                Icon(Icons.Rounded.Shield, contentDescription = "اشتراک", tint = NetShieldColors.Accent)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = when (state.connectionState) {
                ConnectionState.CONNECTED -> "اتصال شما امن است"
                ConnectionState.CONNECTING -> "در حال برقراری اتصال…"
                ConnectionState.DISCONNECTED -> "متصل نیستید"
            },
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(48.dp))

        ConnectButton(
            connected = state.connectionState == ConnectionState.CONNECTED,
            connecting = state.connectionState == ConnectionState.CONNECTING,
            onClick = { viewModel.toggleConnection() }
        )

        Spacer(Modifier.height(48.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(NetShieldColors.Surface)
        ) {
            SettingsRow(
                icon = Icons.Rounded.Dns,
                title = "سرور",
                value = state.selectedConfig?.remark ?: "انتخاب نشده",
                onClick = onOpenLocations
            )
            HorizontalDivider(color = NetShieldColors.Divider)
            SettingsRow(
                icon = Icons.Rounded.LocationOn,
                title = "پروتکل",
                value = state.selectedConfig?.protocol?.name ?: "-",
                onClick = onOpenLocations
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NetShieldColors.Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = NetShieldColors.TextSecondary, fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = NetShieldColors.TextSecondary)
    }
}
