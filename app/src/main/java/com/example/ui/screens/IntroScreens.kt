package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ManaVahanaViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: ManaVahanaViewModel,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val isOnboarded by viewModel.isOnboardingCompleted.collectAsState()
    val isPinEnabled by viewModel.isPinLockEnabled.collectAsState()

    LaunchedEffect(Unit) {
        delay(2000) // Beautiful splash hold
        if (!isOnboarded) {
            onNavigateToOnboarding()
        } else if (isPinEnabled) {
            onNavigateToLogin()
        } else {
            viewModel.bypassPinVerification()
            onNavigateToDashboard()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = list(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Symbolic Wheel with Golden shine
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "మన",
                    color = Color(0xFFFFD700),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ManaVahana",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "మన వాహనం - మన బాధ్యత", // "Our vehicle - Our responsibility" in Telugu
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFFFFD700),
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun <T> list(vararg elements: T): List<T> = elements.toList()

@Composable
fun OnboardingScreen(
    viewModel: ManaVahanaViewModel,
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var pinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    var errorPinMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            label = "onboarding_step_anim"
        ) { currentStep ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .navigationBarsPadding()
                    .statusBarsPadding(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (currentStep) {
                    1 -> {
                        Surface(
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("వాహన", fontSize = 42.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Welcome to ManaVahana",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "A modern vehicle expense tool inspired by Telugu design aesthetics. Track fuel logs, service history, and expiries securely on-device.",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("onboarding_next_1"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                    2 -> {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Secure Your Car Data",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Set a 4-digit security PIN lock to safeguard your vehicle credentials and document files offline.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = pinText,
                            onValueChange = { if (it.length <= 4) pinText = it },
                            label = { Text("Enter 4-digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_pin_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = confirmPinText,
                            onValueChange = { if (it.length <= 4) confirmPinText = it },
                            label = { Text("Confirm PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_pin_confirm")
                        )

                        if (errorPinMsg != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorPinMsg!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    // Skip PIN setup
                                    viewModel.completeOnboarding(null)
                                    onComplete()
                                },
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Skip Security", color = MaterialTheme.colorScheme.primary)
                            }

                            Button(
                                onClick = {
                                    if (pinText.length != 4) {
                                        errorPinMsg = "PIN must be exactly 4 digits"
                                    } else if (pinText != confirmPinText) {
                                        errorPinMsg = "PINs do not match"
                                    } else {
                                        viewModel.completeOnboarding(pinText)
                                        onComplete()
                                    }
                                },
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(52.dp)
                                    .testTag("onboarding_secure_btn")
                            ) {
                                Text("Secure App")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    viewModel: ManaVahanaViewModel,
    onSuccess: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    val savedPin by viewModel.savedSecurityPin.collectAsState()

    var showFingerprintDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "మన వాహనం",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Serif
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter Security PIN",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Visual Dots Indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val filled = i < pinText.length
                val color = if (pinError) {
                    MaterialTheme.colorScheme.error
                } else if (filled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Custom Keypad
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val keys = list(
                list("1", "2", "3"),
                list("4", "5", "6"),
                list("7", "8", "9"),
                list("FP", "0", "DEL")
            )

            for (row in keys) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (key in row) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.8f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    pinError = false
                                    when (key) {
                                        "DEL" -> {
                                            if (pinText.isNotEmpty()) {
                                                pinText = pinText.substring(0, pinText.length - 1)
                                            }
                                        }
                                        "FP" -> {
                                            showFingerprintDialog = true
                                        }
                                        else -> {
                                            if (pinText.length < 4) {
                                                pinText += key
                                                if (pinText.length == 4) {
                                                    // Auto-verify
                                                    if (viewModel.verifyPin(pinText)) {
                                                        onSuccess()
                                                    } else {
                                                        pinError = true
                                                        pinText = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                .testTag("pin_key_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "DEL") {
                                Text("⌫", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else if (key == "FP") {
                                Icon(Icons.Default.Fingerprint, contentDescription = "Biometric Lock", tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Text(
                                    text = key,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFingerprintDialog) {
        AlertDialog(
            onDismissRequest = { showFingerprintDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Biometric Authentication")
                }
            },
            text = {
                Text("Confirm fingerprint or face scan sensor for offline access to ManaVahana.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFingerprintDialog = false
                        viewModel.bypassPinVerification()
                        onSuccess()
                    }
                ) {
                    Text("Authenticate (Simulate)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFingerprintDialog = false }) {
                    Text("Use PIN")
                }
            }
        )
    }
}
