package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.ManaVahanaViewModel
import com.example.ui.ManaVahanaViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.ManaVahanaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ManaVahanaApplication
        val repository = app.repository
        val preferencesRepository = app.userPreferencesRepository

        val factory = ManaVahanaViewModelFactory(repository, preferencesRepository)
        val viewModel = ViewModelProvider(this, factory)[ManaVahanaViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            ManaVahanaTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                val isPinVerified by viewModel.isPinVerified.collectAsState()
                val isPinEnabled by viewModel.isPinLockEnabled.collectAsState()
                val currentUser by viewModel.currentUserState.collectAsState()

                // List of destinations that require the Bottom Navigation Bar
                val bottomNavDestinations = listOf(
                    "dashboard",
                    "fuel_logs",
                    "service_logs",
                    "expenses",
                    "document_vault",
                    "settings"
                )

                val showBottomBar = currentRoute in bottomNavDestinations && (isPinVerified || !isPinEnabled) && currentUser != null

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                modifier = Modifier.testTag("app_navigator")
                            ) {
                                val navItems = listOf(
                                    BottomNavItem("Dashboard", "dashboard", Icons.Default.Dashboard),
                                    BottomNavItem("Fuel", "fuel_logs", Icons.Default.LocalGasStation),
                                    BottomNavItem("Services", "service_logs", Icons.Default.Build),
                                    BottomNavItem("Expenses", "expenses", Icons.Default.Payments),
                                    BottomNavItem("Vault", "document_vault", Icons.Default.FolderZip),
                                    BottomNavItem("Settings", "settings", Icons.Default.Settings)
                                )

                                navItems.forEach { item ->
                                    val isSelected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        modifier = Modifier.testTag("nav_item_${item.route}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash") {
                            SplashScreen(
                                viewModel = viewModel,
                                onNavigateToOnboarding = {
                                    navController.navigate("onboarding") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
                                onNavigateToDashboard = {
                                    navController.navigate("dashboard") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("onboarding") {
                            OnboardingScreen(
                                viewModel = viewModel,
                                onComplete = {
                                    navController.navigate("dashboard") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("login") {
                            PinLockScreen(
                                viewModel = viewModel,
                                onSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToAddVehicle = { navController.navigate("add_vehicle") },
                                onNavigateToVehicleDetails = { id -> navController.navigate("vehicle_details/$id") },
                                onNavigateToAddExpense = { navController.navigate("expenses") },
                                onNavigateToAddService = { navController.navigate("service_logs") },
                                onNavigateToAddFuel = { navController.navigate("fuel_logs") }
                            )
                        }

                        composable("add_vehicle") {
                            AddVehicleScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "vehicle_details/{vehicleId}",
                            arguments = listOf(navArgument("vehicleId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val vehicleId = backStackEntry.arguments?.getInt("vehicleId") ?: 0
                            VehicleDetailsScreen(
                                viewModel = viewModel,
                                vehicleId = vehicleId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("service_logs") {
                            ServiceLogsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("fuel_logs") {
                            FuelLogsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("expenses") {
                            ExpensesScreen(
                                viewModel = viewModel
                            )
                        }

                        composable("document_vault") {
                            DocumentVaultScreen(
                                viewModel = viewModel
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)
