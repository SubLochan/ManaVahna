package com.manavahana

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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.manavahana.ui.ManaVahanaViewModel
import com.manavahana.ui.ManaVahanaViewModelFactory
import com.manavahana.ui.screens.*
import com.manavahana.ui.theme.ManaVahanaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Trigger automatic Google Play Store In-App App Update check on startup
        try {
            com.manavahana.ui.AppUpdateHelper.getInstance(this).checkForUpdates()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Trigger immediate background reminder & document expiry check on startup
        try {
            val reminderWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.manavahana.worker.ReminderWorker>().build()
            androidx.work.WorkManager.getInstance(this).enqueue(reminderWorkRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Request notification permission for Android 13+ (API 33) to allow update alerts on status bar
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1012
                )
            }
        }

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
            ManaVahanaTheme(themeMode = themeMode, darkTheme = isDarkTheme) {
                val selectedLanguage by viewModel.selectedLanguage.collectAsState()
                val langCode = selectedLanguage ?: "en"

                CompositionLocalProvider(com.manavahana.ui.LocalAppLanguage provides langCode) {
                    val navController = rememberNavController()
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route

                    val isPinVerified by viewModel.isPinVerified.collectAsState()
                    val isPinEnabled by viewModel.isPinLockEnabled.collectAsState()
                    val currentUser by viewModel.currentUserState.collectAsState()

                    // Secure Session Guard: Automatically redirect to AuthScreen when user is logged out (JWT is null)
                    LaunchedEffect(currentUser, currentRoute) {
                        if (currentUser == null &&
                            currentRoute != null &&
                            currentRoute != "splash" &&
                            currentRoute != "language_selection" &&
                            currentRoute != "onboarding" &&
                            currentRoute != "auth"
                        ) {
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }

                // List of destinations that require the Bottom Navigation Bar
                val bottomNavDestinations = listOf(
                    "dashboard",
                    "fuel_logs",
                    "service_logs",
                    "expenses",
                    "document_vault",
                    "settings"
                )

                var showQuickActions by remember { mutableStateOf(false) }
                val showBottomBar = currentRoute in bottomNavDestinations && (isPinVerified || !isPinEnabled) && currentUser != null

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (showBottomBar) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .testTag("app_navigator")
                            ) {
                                // Bottom Navigation bar capsule
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                                        .height(72.dp)
                                        .align(Alignment.BottomCenter),
                                    shape = RoundedCornerShape(percent = 50),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                                    tonalElevation = 8.dp,
                                    shadowElevation = 12.dp
                                ) {
                                    val leftItems = listOf(
                                        BottomNavItem(com.manavahana.ui.Localizer.get("nav_dashboard", langCode), "dashboard", Icons.Default.Dashboard),
                                        BottomNavItem(com.manavahana.ui.Localizer.get("nav_expenses", langCode), "expenses", Icons.Default.Payments)
                                    )
                                    val rightItems = listOf(
                                        BottomNavItem(com.manavahana.ui.Localizer.get("nav_vault", langCode), "document_vault", Icons.Default.FolderZip),
                                        BottomNavItem(com.manavahana.ui.Localizer.get("nav_settings", langCode), "settings", Icons.Default.Settings)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left side items
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            leftItems.forEach { item ->
                                                val isSelected = currentRoute == item.route
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(24.dp))
                                                        .clickable {
                                                            if (currentRoute != item.route) {
                                                                navController.navigate(item.route) {
                                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                                    launchSingleTop = true
                                                                    restoreState = true
                                                                }
                                                            }
                                                        }
                                                        .padding(10.dp)
                                                        .testTag("nav_item_${item.route}"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = item.label,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(30.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // FAB spacer
                                        Spacer(modifier = Modifier.width(76.dp))

                                        // Right side items
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            rightItems.forEach { item ->
                                                val isSelected = currentRoute == item.route
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(24.dp))
                                                        .clickable {
                                                            if (currentRoute != item.route) {
                                                                navController.navigate(item.route) {
                                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                                    launchSingleTop = true
                                                                    restoreState = true
                                                                }
                                                            }
                                                        }
                                                        .padding(10.dp)
                                                        .testTag("nav_item_${item.route}"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = item.label,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(30.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Centered Overlay Floating Action Button
                                FloatingActionButton(
                                    onClick = { showQuickActions = true },
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = (-20).dp)
                                        .size(54.dp)
                                        .testTag("center_floating_fab"),
                                    shape = CircleShape,
                                    containerColor = Color(0xFFFFA000), // custom warning golden exactly like screenshot
                                    contentColor = Color.Black,
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = "Quick Action Car Menu",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    if (showQuickActions) {
                        Dialog(onDismissRequest = { showQuickActions = false }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = if (langCode == "te") "వాహన త్వరిత చర్యలు" else if (langCode == "hi") "वाहन त्वरित कार्रवाई" else "Vehicle Quick Actions",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    val actionsList = listOf(
                                        Triple(
                                            if (langCode == "te") "వాహనం జోడించు" else if (langCode == "hi") "वाहन शामिल करें" else "Add Vehicle",
                                            "add_vehicle",
                                            Icons.Default.DirectionsCar
                                        ),
                                        Triple(
                                            if (langCode == "te") "ఖర్చు జోడించు" else if (langCode == "hi") "खर्च शामिल करें" else "Add Expense",
                                            "expenses",
                                            Icons.Default.Payments
                                        ),
                                        Triple(
                                            if (langCode == "te") "సర్వీస్ రికార్డ్ జోడించు" else if (langCode == "hi") "सेवा रिकॉर्ड शामिल करें" else "Add Service Record",
                                            "service_logs",
                                            Icons.Default.Build
                                        ),
                                        Triple(
                                            if (langCode == "te") "ఇంధనం పూరించండి" else if (langCode == "hi") "ईंधन भरें" else "Fill Fuel",
                                            "fuel_logs",
                                            Icons.Default.LocalGasStation
                                        )
                                    )

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        actionsList.forEach { (label, route, icon) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .clickable {
                                                        showQuickActions = false
                                                        navController.navigate(route)
                                                    }
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = label,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                androidx.compose.material3.Text(
                                                    text = label,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    TextButton(
                                        onClick = { showQuickActions = false },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        androidx.compose.material3.Text(
                                            text = if (langCode == "te") "మూసివేయి" else if (langCode == "hi") "बंद करें" else "Close",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash") {
                            SplashScreen(
                                viewModel = viewModel,
                                onNavigateToLanguageSelection = {
                                    navController.navigate("language_selection") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
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

                        composable("language_selection") {
                            LanguageSelectionScreen(
                                viewModel = viewModel,
                                onLangSelected = {
                                    val isOnboarded = viewModel.isOnboardingCompleted.value
                                    val isPinEnabled = viewModel.isPinLockEnabled.value
                                    if (!isOnboarded) {
                                        navController.navigate("onboarding") {
                                            popUpTo("language_selection") { inclusive = true }
                                        }
                                    } else if (isPinEnabled) {
                                        navController.navigate("login") {
                                            popUpTo("language_selection") { inclusive = true }
                                        }
                                    } else {
                                        viewModel.bypassPinVerification()
                                        navController.navigate("dashboard") {
                                            popUpTo("language_selection") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable("onboarding") {
                            OnboardingScreen(
                                viewModel = viewModel,
                                onComplete = {
                                    navController.navigate("auth") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("auth") {
                            AuthScreen(
                                viewModel = viewModel,
                                onAuthSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("auth") { inclusive = true }
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
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)
