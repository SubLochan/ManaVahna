package com.manavahana.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Logout
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import com.manavahana.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manavahana.ui.ManaVahanaViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: ManaVahanaViewModel,
    onNavigateToLanguageSelection: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val isOnboarded by viewModel.isOnboardingCompleted.collectAsState()
    val isPinEnabled by viewModel.isPinLockEnabled.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val langCode = selectedLanguage ?: "en"

    LaunchedEffect(Unit) {
        delay(2000) // Beautiful splash hold
        if (selectedLanguage == null) {
            onNavigateToLanguageSelection()
        } else if (!isOnboarded) {
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
                    .size(110.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .border(2.dp, Color(0xFFFFD700).copy(alpha = 0.3f), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.telugu_vehicle_app_icon_1779703541893),
                    contentDescription = "ManaVahana Logo",
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
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
                text = if (langCode == "te") "మన వాహనం - మన బాధ్యత" else if (langCode == "hi") "हमारी गाड़ी - हमारी जिम्मेदारी" else "Our vehicle - Our responsibility",
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
    
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val langCode = selectedLanguage ?: "en"

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
                            color = Color(0xFF1B1B1D),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.telugu_vehicle_app_icon_1779703541893),
                                    contentDescription = "ManaVahana App Icon",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = if (langCode == "te") "మనవాహనకు స్వాగతం" else if (langCode == "hi") "मानवाहन में आपका स्वागत है" else "Welcome to ManaVahana",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (langCode == "te") {
                                "తెలుగు డిజైన్ శైలితో రూపొందించబడిన ఆధునిక వాహన ఖర్చుల సాధనం. మీ మొబైల్‌లోనే ఇంధన లాగ్‌లు, సర్వీస్ హిస్టరీ మరియు గడువు తేదీలను సురక్షితంగా ట్రాక్ చేయండి."
                            } else if (langCode == "hi") {
                                "तेलुगु डिजाइन सौंदर्यशास्त्र से प्रेरित आधुनिक वाहन व्यय उपकरण। ईंधन लॉग, सेवा इतिहास और समाप्ति तिथियों को डिवाइस पर सुरक्षित रूप से ट्रैक करें।"
                            } else {
                                "A modern vehicle expense tool inspired by Telugu design aesthetics. Track fuel logs, service history, and expiries securely on-device."
                            },
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        Button(
                            onClick = {
                                step = 2
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("onboarding_next_1"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = if (langCode == "te") "ప్రారంభించండి" else if (langCode == "hi") "शुरू करें" else "Get Started",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
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
                            text = if (langCode == "te") "మీ వాహన డేటాను సురక్షితం చేసుకోండి" else if (langCode == "hi") "अपने वाहन डेटा को सुरक्षित करें" else "Secure Your Car Data",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (langCode == "te") {
                                "మీ వాహనం ఆధారాలు మరియు డాక్యుమెంట్ ఫైళ్లను ఆఫ్‌లైన్‌లో భద్రపరచడానికి 4-అంకెల సెక్యూరిటీ పిన్ లాక్‌ని సెట్ చేయండి."
                            } else if (langCode == "hi") {
                                "ऑफ़लाइन रहते हुए अपने वाहन क्रेडेंशियल और दस्तावेज़ फ़ाइलों को सुरक्षित रखने के लिए 4-अंकीय सुरक्षा पिन लॉक सेट करें।"
                            } else {
                                "Set a 4-digit security PIN lock to safeguard your vehicle credentials and document files offline."
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = pinText,
                            onValueChange = { if (it.length <= 4) pinText = it },
                            label = { 
                                Text(
                                    if (langCode == "te") "4-అంకెల పిన్ సమర్పించండి" else if (langCode == "hi") "4-अंकीय पिन दर्ज करें" else "Enter 4-digit PIN"
                                ) 
                            },
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
                            label = { 
                                Text(
                                    if (langCode == "te") "పిన్ నిర్ధారించండి" else if (langCode == "hi") "पिन की पुष्टि करें" else "Confirm PIN"
                                ) 
                            },
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
                                Text(
                                    text = if (langCode == "te") "భద్రత దాటవేయి" else if (langCode == "hi") "सुरक्षा छोड़ें" else "Skip Security",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = {
                                    if (pinText.length != 4) {
                                        errorPinMsg = if (langCode == "te") "పిన్ ఖచ్చితంగా 4 అంకెలు ఉండాలి" else if (langCode == "hi") "पिन ठीक 4 अंकों का होना चाहिए" else "PIN must be exactly 4 digits"
                                    } else if (pinText != confirmPinText) {
                                        errorPinMsg = if (langCode == "te") "పిన్ సరిపోలడం లేదు" else if (langCode == "hi") "पिन मेल नहीं खाते" else "PINs do not match"
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
                                Text(
                                    if (langCode == "te") "యాప్‌ను సురక్షితం చేయి" else if (langCode == "hi") "ऐप सुरक्षित करें" else "Secure App"
                                )
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
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val langCode = selectedLanguage ?: "en"

    var showFingerprintDialog by remember { mutableStateOf(false) }
    val isFingerprintEnabled by viewModel.isFingerprintEnabled.collectAsState()

    LaunchedEffect(isFingerprintEnabled) {
        if (isFingerprintEnabled) {
            showFingerprintDialog = true
        }
    }

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
            text = if (langCode == "te") "మన వాహనం" else if (langCode == "hi") "मानवाहन" else "ManaVahana",
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

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showFingerprintDialog) {
        AlertDialog(
            onDismissRequest = { showFingerprintDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (langCode == "te") "బయోమెట్రిక్ ప్రామాణీకరణ" else if (langCode == "hi") "बायोमेट्रिक प्रमाणीकरण" else "Biometric Authentication"
                    )
                }
            },
            text = {
                Text(
                    if (langCode == "te") {
                        "మనవాహనకు ఆఫ్‌లైన్ యాక్సెస్ కోసం వేలిముద్ర లేదా ఫేస్ స్కాన్ సెన్సార్‌ను ధృవీకరించండి."
                    } else if (langCode == "hi") {
                        "मानवाहन तक ऑफ़लाइन पहुंच के लिए फिंगरप्रिंट या फेस स्कैन सेंसर की पुष्टि करें।"
                    } else {
                        "Confirm fingerprint or face scan sensor for offline access to ManaVahana."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFingerprintDialog = false
                        viewModel.bypassPinVerification()
                        onSuccess()
                    }
                ) {
                    Text(
                        if (langCode == "te") "ధృవీకరించండి" else if (langCode == "hi") "प्रमाणित करें" else "Confirm"
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showFingerprintDialog = false }) {
                    Text(
                        if (langCode == "te") "పిన్ ఉపయోగించండి" else if (langCode == "hi") "पिन का उपयोग करें" else "Use PIN"
                    )
                }
            }
        )
    }
}

@Composable
fun LanguageSelectionScreen(
    viewModel: ManaVahanaViewModel,
    onLangSelected: () -> Unit
) {
    var selectedVal by remember { mutableStateOf("en") }
    val scrollState = rememberScrollState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .widthIn(max = 500.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main content containing header and language options list, made vertically scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(90.dp),
                        shape = CircleShape,
                        color = Color(0xFF1B1B1D),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.telugu_vehicle_app_icon_1779703541893),
                                contentDescription = "ManaVahana Logo",
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Select Language / భాషను ఎంచుకోండి",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "దయచేసి మీ ప్రాధాన్యత భాషను ఎంచుకోండి.\nPlease select your preferred language.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                // Language Options Cards List
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    com.manavahana.ui.Localizer.LANGUAGES.forEach { option ->
                        val isSelected = selectedVal == option.code
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVal = option.code }
                                .testTag("lang_option_${option.code}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = option.displayName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = option.englishName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedVal = option.code },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Button (Pinned securely at the bottom)
            Button(
                onClick = {
                    viewModel.selectLanguage(selectedVal)
                    onLangSelected()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("confirm_language_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = when (selectedVal) {
                        "te" -> "కొనసాగించు (Continue)"
                        "hi" -> "जारी रखें (Continue)"
                        else -> "Continue (కొనసాగించు)"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
