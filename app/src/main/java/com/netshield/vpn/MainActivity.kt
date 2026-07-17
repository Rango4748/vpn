package com.netshield.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.netshield.vpn.ui.screens.LocationsScreen
import com.netshield.vpn.ui.screens.SettingsScreen
import com.netshield.vpn.ui.screens.StatusScreen
import com.netshield.vpn.ui.screens.StatusViewModel
import com.netshield.vpn.ui.screens.SubscriptionScreen
import com.netshield.vpn.ui.theme.NetShieldColors
import com.netshield.vpn.ui.theme.NetShieldTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetShieldTheme {
                NetShieldApp()
            }
        }
    }
}

private sealed class Dest(val route: String, val label: String) {
    data object Status : Dest("status", "وضعیت")
    data object Locations : Dest("locations", "سرورها")
    data object Settings : Dest("settings", "تنظیمات")
    data object Subscription : Dest("subscription", "اشتراک")
}

@Composable
private fun NetShieldApp() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = NetShieldColors.Background,
        bottomBar = { BottomBar(navController) }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController, startDestination = Dest.Status.route) {
                composable(Dest.Status.route) {
                    val statusViewModel: StatusViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        if (result.resultCode == android.app.Activity.RESULT_OK) {
                            statusViewModel.onPermissionGranted()
                        } else {
                            statusViewModel.onPermissionDenied()
                        }
                    }
                    val pendingPermission by statusViewModel.pendingPermissionRequest.collectAsState()
                    LaunchedEffect(pendingPermission) {
                        pendingPermission?.let { permissionLauncher.launch(it) }
                    }

                    StatusScreen(
                        onOpenLocations = { navController.navigate(Dest.Locations.route) },
                        onOpenSubscription = { navController.navigate(Dest.Subscription.route) },
                        viewModel = statusViewModel
                    )
                }
                composable(Dest.Locations.route) { LocationsScreen() }
                composable(Dest.Settings.route) { SettingsScreen() }
                composable(Dest.Subscription.route) { SubscriptionScreen() }
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(containerColor = NetShieldColors.Surface) {
        listOf(
            Triple(Dest.Status, Icons.Rounded.Shield, Dest.Status.label),
            Triple(Dest.Locations, Icons.Rounded.List, Dest.Locations.label),
            Triple(Dest.Settings, Icons.Rounded.Settings, Dest.Settings.label)
        ).forEach { (dest, icon, label) ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(Dest.Status.route)
                        launchSingleTop = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NetShieldColors.Accent,
                    selectedTextColor = NetShieldColors.Accent,
                    unselectedIconColor = NetShieldColors.TextSecondary,
                    unselectedTextColor = NetShieldColors.TextSecondary,
                    indicatorColor = NetShieldColors.SurfaceVariant
                )
            )
        }
    }
}
