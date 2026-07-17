package com.netshield.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netshield.vpn.data.model.PanelType
import com.netshield.vpn.data.model.ServerConfig
import com.netshield.vpn.ui.theme.NetShieldColors

private enum class AddMode { MANUAL, PANEL, SUBSCRIPTION }

@Composable
fun LocationsScreen(vm: LocationsViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var mode by remember { mutableStateOf(AddMode.MANUAL) }
    var manualLink by remember { mutableStateOf("") }
    var panelUrl by remember { mutableStateOf("") }
    var panelUser by remember { mutableStateOf("") }
    var panelPass by remember { mutableStateOf("") }
    var subUrl by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage, state.infoMessage) {
        (state.errorMessage ?: state.infoMessage)?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMessages()
        }
    }

    Scaffold(
        containerColor = NetShieldColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text("سرورها", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))

            // Mode switch: manual link vs panel vs subscription
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NetShieldColors.Surface)
                    .padding(4.dp)
            ) {
                ModeTab("دستی", mode == AddMode.MANUAL) { mode = AddMode.MANUAL }
                ModeTab("پنل", mode == AddMode.PANEL) { mode = AddMode.PANEL }
                ModeTab("سابسکریپشن", mode == AddMode.SUBSCRIPTION) { mode = AddMode.SUBSCRIPTION }
            }

            Spacer(Modifier.height(16.dp))

            when (mode) {
                AddMode.MANUAL -> {
                    OutlinedTextField(
                        value = manualLink,
                        onValueChange = { manualLink = it },
                        label = { Text("لینک کانفیگ (vmess:// vless:// trojan:// ss://)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.addManualLink(manualLink); manualLink = "" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NetShieldColors.Accent)
                    ) { Text("افزودن کانفیگ", color = androidx.compose.ui.graphics.Color.Black) }
                }
                AddMode.PANEL -> {
                    OutlinedTextField(panelUrl, { panelUrl = it }, label = { Text("آدرس پنل (https://panel.example.com)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(panelUser, { panelUser = it }, label = { Text("نام کاربری") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(panelPass, { panelPass = it }, label = { Text("رمز عبور") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.connectPanel(panelUrl, panelUser, panelPass, PanelType.MARZBAN) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NetShieldColors.Accent)
                    ) { Text("اتصال به پنل و دریافت سرورها", color = androidx.compose.ui.graphics.Color.Black) }
                }
                AddMode.SUBSCRIPTION -> {
                    OutlinedTextField(subUrl, { subUrl = it }, label = { Text("لینک سابسکریپشن") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.addSubscription(subUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NetShieldColors.Accent)
                    ) { Text("دریافت لیست سرورها", color = androidx.compose.ui.graphics.Color.Black) }
                }
            }

            if (state.loading) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NetShieldColors.Accent)
            }

            Spacer(Modifier.height(20.dp))
            Text("سرورهای ذخیره‌شده", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn {
                items(state.configs) { config ->
                    ServerRow(config, onDelete = { vm.removeConfig(config) })
                    HorizontalDivider(color = NetShieldColors.Divider)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) NetShieldColors.Accent else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) androidx.compose.ui.graphics.Color.Black else NetShieldColors.TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ServerRow(config: ServerConfig, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(config.remark, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Medium)
            Text("${config.protocol} · ${config.address}:${config.port} · ${config.source}", color = NetShieldColors.TextSecondary, fontSize = 12.sp)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = NetShieldColors.Danger)
        }
    }
}
