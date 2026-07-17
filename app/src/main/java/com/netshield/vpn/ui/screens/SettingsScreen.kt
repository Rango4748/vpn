package com.netshield.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.netshield.vpn.ui.theme.NetShieldColors

@Composable
fun SettingsScreen() {
    var autoConnect by remember { mutableStateOf(true) }
    var killSwitch by remember { mutableStateOf(true) }
    var startOnBoot by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(NetShieldColors.Background)
            .padding(20.dp)
    ) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        SettingsSwitchRow("اتصال خودکار هنگام باز شدن اپ", autoConnect) { autoConnect = it }
        SettingsSwitchRow("کیل‌سوییچ (قطع اینترنت هنگام قطع VPN)", killSwitch) { killSwitch = it }
        SettingsSwitchRow("اجرا هنگام روشن شدن گوشی", startOnBoot) { startOnBoot = it }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = NetShieldColors.Accent)
        )
    }
}
