package com.manavahana.ui

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppLanguage = staticCompositionLocalOf { "en" }

object Localizer {
    
    val LANGUAGES = listOf(
        LangOption("en", "English", "English"),
        LangOption("te", "తెలుగు", "Telugu"),
        LangOption("hi", "हिन्दी", "Hindi")
    )

    data class LangOption(val code: String, val displayName: String, val englishName: String)

    private val translations = mapOf(
        "en" to mapOf(
            "app_title" to "ManaVahana",
            "app_tagline" to "Our vehicle - Our responsibility",
            "select_language_title" to "Select Language",
            "select_language_subtitle" to "దయచేసి మీ ప్రాధాన్యత భాషను ఎంచుకోండి.\nPlease select your preferred language.",
            "lang_english" to "English",
            "lang_telugu" to "తెలుగు (Telugu)",
            "continue_btn" to "Continue / కొనసాగించు",
            "settings_lang_title" to "Language Settings",
            "settings_lang_desc" to "Choose your default application language.",
            "toast_lang_changed" to "Language updated to English successfully!",
            
            // Bottom navigation
            "nav_dashboard" to "Dashboard",
            "nav_fuel" to "Fuel",
            "nav_services" to "Services",
            "nav_expenses" to "Expenses",
            "nav_vault" to "Vault",
            "nav_settings" to "Settings",

            // Settings labels
            "settings_title" to "Settings",
            "settings_subtitle" to "Configure offline-first security & local database storage preferences.",
            "security_settings" to "Security Settings",
            "secure_pin_lock" to "Secure PIN Lock",
            "pin_start_desc" to "Ask for PIN on app startup",
            "biometric_auth" to "Biometric Authentication",
            "biometric_desc" to "Enable fingerprint scan unlock",
            "visual_appearance" to "Visual Appearance",
            "visual_appearance_desc" to "Set your visual preference for ManaVahana. Pick Standard Light, Eye-Safe Dark, or follow System Settings.",
            "theme_light" to "Light Mode",
            "theme_dark" to "Dark Mode",
            "theme_system" to "System",
            
            // Dashboard labels
            "total_vehicles" to "Total Vehicles",
            "month_expenses" to "Month Expenses",
            "selected_vehicle" to "Selected Vehicle",
            "last_odometer" to "Last Odometer",
            "monthly_spending" to "Monthly Expense Flow",
            "current_month" to "Current Month Logged",
            "reminders_due" to "Reminders",
            "active_reminders_count" to "Active Reminders",
            "add_vehicle" to "Add Vehicle",
            "reminders_header" to "Reminders & To-Dos",
            "doc_expiry_system" to "Reminder & Expiry Alert System",
            "scans_alert_desc" to "ManaVahana schedules secure background tasks to parse your vehicle insurance, pollution certificates, and personal document vaults. Get warned instantly on your status bar for upcoming dates and overdue tasks completely offline.",
            "trigger_scan" to "Trigger Expiration Scan & Alerts"
        ),
        "te" to mapOf(
            "app_title" to "మనవాహన",
            "app_tagline" to "మన వాహనం - మన బాధ్యత",
            "select_language_title" to "భాషను ఎంచుకోండి",
            "select_language_subtitle" to "దయచేసి మీ ప్రాధాన్యత భాషను ఎంచుకోండి.\nPlease select your preferred language.",
            "lang_english" to "English",
            "lang_telugu" to "తెలుగు (Telugu)",
            "continue_btn" to "కొనసాగించు / Continue",
            "settings_lang_title" to "భాష సెట్టింగులు (Language)",
            "settings_lang_desc" to "మీ డిఫాల్ట్ అప్లికేషన్ భాషను ఎంచుకోండి.",
            "toast_lang_changed" to "భాష విజయవంతంగా తెలుగుకు సెట్ చేయబడింది!",
            
            // Bottom navigation
            "nav_dashboard" to "డాష్‌బోర్డ్",
            "nav_fuel" to "ఇంధనం",
            "nav_services" to "సర్వీసులు",
            "nav_expenses" to "ఖర్చులు",
            "nav_vault" to "వాల్ట్",
            "nav_settings" to "అమరికలు",

            // Settings labels
            "settings_title" to "అమరికలు (Settings)",
            "settings_subtitle" to "ఆఫ్‌లైన్ భద్రత మరియు లోకల్ డేటాబేస్ ప్రాధాన్యతలను కాన్ఫిగర్ చేయండి.",
            "security_settings" to "భద్రతా సెట్టింగులు",
            "secure_pin_lock" to "భద్రతా పిన్ లాక్",
            "pin_start_desc" to "యాప్ స్టార్టప్‌లో పిన్ అడగండి",
            "biometric_auth" to "బయోమెట్రిక్ ప్రామాణీకరణ",
            "biometric_desc" to "ఫింగర్‌ప్రింట్ స్కాన్ అన్‌లాక్ ఎనేబుల్ చేయండి",
            "visual_appearance" to "కనిపించే రూపం",
            "visual_appearance_desc" to "మనవాహన కోసం మీ థీమ్ ప్రాధాన్యతను సెట్ చేయండి. లైట్, ఐ-సేఫ్ డార్క్ లేదా సిస్టమ్ సెట్టింగులను ఎంచుకోండి.",
            "theme_light" to "లైట్ మోడ్",
            "theme_dark" to "డార్క్ మోడ్",
            "theme_system" to "సిస్టమ్",
            
            // Dashboard labels
            "total_vehicles" to "మొత్తం వాహనాలు",
            "month_expenses" to "నెలవారీ ఖర్చులు",
            "selected_vehicle" to "ఎంచుకున్న వాహనం",
            "last_odometer" to "చివరి ఓడోమీటర్",
            "monthly_spending" to "నెలవారీ ఖర్చుల ఫ్లో",
            "current_month" to "ప్రస్తుత నెలలో నమోదు చేసినవి",
            "reminders_due" to "రిమైండర్లు",
            "active_reminders_count" to "క్రియాశీల రిమైండర్లు",
            "add_vehicle" to "వాహనం జోడించు",
            "reminders_header" to "రిమైండర్లు & చేయవలసినవి",
            "doc_expiry_system" to "రిమైండర్ & గడువు అలర్ట్ సిస్టమ్",
            "scans_alert_desc" to "మీ వాహన ఇన్సూరెన్స్, కాలుష్య ధృవీకరణ పత్రాలు మరియు వ్యక్తిగత పత్రాల వాల్ట్‌ను పార్స్ చేయడానికి మనవాహన బ్యాక్‌గ్రౌండ్ టాస్క్‌లను షెడ్యూల్ చేస్తుంది. రాబోయే తేదీలు మరియు ఆలస్యమైన పనుల గురించి మీ స్థితి పట్టీ (Status Bar) పై అలర్ట్‌లను పూర్తిగా ఆఫ్‌లైన్‌లో పొందండి.",
            "trigger_scan" to "ఎక్స్పైరేషన్ స్కాన్ & అలర్ట్‌లను ప్రారంభించండి"
        ),
        "hi" to mapOf(
            "app_title" to "ManaVahana",
            "app_tagline" to "हमारी गाड़ी - हमारी जिम्मेदारी",
            "select_language_title" to "भाषा चुनें",
            "select_language_subtitle" to "कृपया अपनी पसंदीदा भाषा चुनें।\nPlease select your preferred language.",
            "lang_english" to "English",
            "lang_telugu" to "తెలుగు (Telugu)",
            "lang_hindi" to "हिन्दी (Hindi)",
            "continue_btn" to "जारी रखें / Continue",
            "settings_lang_title" to "भाषा सेटिंग्स (Language)",
            "settings_lang_desc" to "अपनी डिफ़ॉल्ट एप्लिकेशन भाषा चुनें।",
            "toast_lang_changed" to "भाषा सफलतापूर्वक हिन्दी में सेट हो गई है!",
            
            // Bottom navigation
            "nav_dashboard" to "डैशबोर्ड",
            "nav_fuel" to "ईंधन",
            "nav_services" to "सेवाएं",
            "nav_expenses" to "खर्चे",
            "nav_vault" to "तिजोरी",
            "nav_settings" to "सेटिंग्स",

            // Settings labels
            "settings_title" to "सेटिंग्स (Settings)",
            "settings_subtitle" to "ऑफ़लाइन-फ़र्स्ट सुरक्षा और स्थानीय डेटाबेस प्राथमिकताएं कॉन्फ़िगर करें।",
            "security_settings" to "सुरक्षा सेटिंग्स",
            "secure_pin_lock" to "सुरक्षित पिन लॉक",
            "pin_start_desc" to "ऐप प्रत्येक बार खुलने पर पिन मांगें",
            "biometric_auth" to "बायोमेट्रिक प्रमाणीकरण",
            "biometric_desc" to "फिंगरप्रिंट स्कैन अनलॉक सक्षम करें",
            "visual_appearance" to "दृश्यात्मक उपस्थिति",
            "visual_appearance_desc" to "मानवाहन के लिए अपनी दृश्य पसंद सेट करें। मानक लाइट, आई-सेफ डार्क, या सिस्टम सेटिंग्स चुनें।",
            "theme_light" to "लाइट मोड",
            "theme_dark" to "डार्क मोड",
            "theme_system" to "सिस्टम",
            
            // Dashboard labels
            "total_vehicles" to "कुल वाहन",
            "month_expenses" to "मासिक खर्च",
            "selected_vehicle" to "चयनित वाहन",
            "last_odometer" to "अंतिम ओडोमीटर",
            "monthly_spending" to "मासिक खर्च प्रवाह",
            "current_month" to "वर्तमान माह लॉग",
            "reminders_due" to "अनुस्मारक",
            "active_reminders_count" to "सक्रिय अनुस्मारक",
            "add_vehicle" to "वाहन शामिल करें",
            "reminders_header" to "अनुस्मारक और कार्य सूची",
            "doc_expiry_system" to "स्मरण पत्र और समाप्ति चेतावनी प्रणाली",
            "scans_alert_desc" to "मानवाहन ऑफ़लाइन रहकर आपके बीमा, क्रेडेंशियल्स और तिजोरी की समाप्ति का विश्लेषण करता है।",
            "trigger_scan" to "समाप्ति तिथि जाँचें"
        )
    )

    private val exactTeluguToEnglish = mapOf(
        "యజమాని (Owner)" to "Owner",
        "యజమాని" to "Owner",
        "నమస్కారం, యజమాని (Owner)!" to "Hello, Owner!",
        "నమస్కారం, Owner!" to "Hello, Owner!",
        "కొత్త అప్‌డేట్ అందుబాటులో ఉంది!" to "New Update Available!",
        "తర్వాత (Later)" to "Later",
        "ఇప్పుడే అప్‌డేట్ చేయి" to "Update Now",
        "నమస్కారం! (Namaskaram)" to "Hello! (Namaskaram)",
        "మాస నివేదిక / Monthly Sheet" to "Monthly Report",
        "అమరికలు (Settings)" to "Settings",
        "భాషను ఎంచుకోండి" to "Select Language",
        "అప్‌డేట్" to "Update",
        "అప్‌డేట్ అందుబాటులో ఉంది!" to "Update available!",
        "దయచేసి మీ ప్రాధాన్యత భాషను ఎంచుకోండి.\nPlease select your preferred language." to "Please select your preferred language.",
        "కొనసాగించు / Continue" to "Continue",
        "కొనసాగించు" to "Continue",
        "కొనసాగించు (Continue)" to "Continue",
        "Continue (కొనసాగించు)" to "Continue",
        "భాష విజయవంతంగా తెలుగుకు సెట్ చేయబడింది!" to "Language updated to Telugu successfully!",
        "భాష విజయవంతంగా ఇంగ్లీష్‌కు సెట్ చేయబడింది!" to "Language updated to English successfully!",
        "భద్రతా పిన్ సృష్టించండి" to "Create Secure PIN",
        "సెక్యూరిటీ పిన్ సెట్ చేయడం ద్వారా మీ డేటా సురక్షితంగా ఉంటుంది." to "Set your security PIN to secure your local database details.",
        "పిన్ నమోదు చేయండి" to "Enter PIN",
        "మనవాహనకు స్వాగతం" to "Welcome to ManaVahana",
        "మన వాహనం - మన బాధ్యత." to "Our vehicle, our responsibility.",
        "నమోదు చేయండి" to "Register",
        "తదుపరి" to "Next",
        "వెనుకకు" to "Back",
        "పూర్తి చేయి" to "Done",
        "ప్రారంభించు" to "Start",
        "డాక్యుమెంట్ వాల్ట్" to "Document Vault",
        "ఆఫ్‌లైన్ డాక్యుమెంట్లు" to "Offline Documents",
        "ఇంధన రికార్డులు" to "Fuel Records",
        "ఖర్చుల రికార్డులు" to "Expense Records",
        "సర్వీస్ హిస్టరీ" to "Service History",
        "సర్వీస్ రికార్డ్స్" to "Service Records",
        "మొత్తం ఖర్చులు" to "Total Expenses",
        "అన్ని లాగ్‌లు" to "All Logs",
        "తేదీ" to "Date",
        "ధర" to "Cost",
        "రిటైల్ స్టేషన్" to "Retail Station",
        "లీటర్లు" to "Liters",
        "రసీదు చిత్రం" to "Receipt Image",
        "ఫోటో తీయండి" to "Capture Photo",
        "గ్యాలరీ నుండి ఎంచుకోండి" to "Choose from Gallery",
        "సేవ్ చేయి" to "Save",
        "రద్దు చేయి" to "Cancel",
        "తొలగించు" to "Delete",
        "సవరించు" to "Edit",
        "వివరాలు" to "Details",
        "గమనికలు" to "Notes",
        "చిరునామా" to "Location Address",
        "యాప్ అప్‌డేట్‌లు" to "App Updates",
        "సహాయం & మద్దతు" to "Help & Support",
        "వాహనం నంబర్" to "Vehicle Number",
        "వాహనం పేరు" to "Vehicle Name",
        "ఇంధన టైప్" to "Fuel Type",
        "లక్షణాలు" to "Features",
        "ఓడోమీటర్" to "Odometer",
        "గురించి" to "About",
        "సిస్టమ్ థీమ్" to "System Theme",
        "డార్క్ థీమ్" to "Dark Theme",
        "లైట్ థీమ్" to "Light Theme",
        "భాషను విజయవంతంగా అప్‌డేట్ చేసాం!" to "Language updated successfully!"
    )

    private val teluguSubstringsMap = mapOf(
        "యజమాని" to "Owner",
        "నమస్కారం" to "Hello",
        "అమరికలు" to "Settings",
        "మైలేజ్ (Mileage)" to "Mileage",
        "మైలేజ్" to "Mileage",
        "కొనసాగించు" to "Continue",
        "తర్వాత" to "Later",
        "ఇప్పుడే" to "Now",
        "అప్‌డేట్" to "Update",
        "సేవ్ చేయి" to "Save",
        "రద్దు చేయి" to "Cancel",
        "తేదీ" to "Date",
        "త్వరలో ముగుస్తుంది" to "will expire soon",
        "గడువు ముగిసింది!" to "expired!",
        "త్వరలో ముగియనుంది" to "will expire soon",
        "గడువు ముగిసింది" to "expired",
        "పొల్యూషన్ సర్టిఫికేట్ అలర్ట్" to "Pollution Certificate Alert",
        "పొల్యూషన్ గడువు ముగిసింది!" to "Pollution Expired!",
        "ఇన్సూరెన్స్ త్వరలో ముగియనుంది" to "Insurance will expire soon",
        "ఇన్సూరెన్స్ గడువు ముగిసింది!" to "Insurance expired!",
        "మీ డాక్యుమెంట్ గడువు" to "Your document is about to expire",
        "తేదీతో ముగియనుంది. ఇప్పుడే రిన్యూ చేసుకోండి!" to "is the expiry date. Renew now!",
        "తేదీతో ముగిసింది. దయచేసి వెంటనే రిన్యూ చేయండి!" to "expired. Please renew immediately!",
        "తో ముగియనుంది. తనిఖీ చేయించుకోండి!" to "is the expiry date. Please test it!",
        "నాటికి ముగిసింది. వెంటనే కొత్తది పొందండి!" to "expired. Please obtain a new certificate!",
        "అలర్ట్" to "Alert",
        "త్వరలో ఉంది/Upcoming" to "Upcoming",
        "గడువు ముగిసింది/Overdue" to "Overdue",
        "త్వరలో ఉంది" to "Upcoming",
        "బియాండ్" to "Beyond",
        "త్వరలో ముగుస్తుంది" to "Expires Soon",
        "మీ సేవ్ చేసిన డాక్యుమెంట్" to "Your saved document",
        "తీదీతో ముగిసింది. దయచేసి అప్‌డేట్ చేయండి!" to "expired. Please update it!",
        "తేదీతో ముగిసింది. దయచేసి అప్‌డేట్ చేయండి!" to "expired. Please update it!"
    )

    private val exactTeluguToHindi = mapOf(
        "యజమాని (Owner)" to "मालिक (Owner)",
        "యజమాని" to "मालिक",
        "నమస్కారం, యజమాని (Owner)!" to "नमस्ते, मालिक!",
        "నమస్కారం, Owner!" to "नमस्ते, Owner!",
        "కొత్త అప్‌డేట్ అందుబాటులో ఉంది!" to "नया अपडेट उपलब्ध है!",
        "తర్వాత (Later)" to "बाद में (Later)",
        "ఇప్పుడే అప్‌డేట్ చేయి" to "अभी अपडेट करें",
        "నమస్కారం! (Namaskaram)" to "नमस्ते! (Namaskaram)",
        "మాస నివేదిక / Monthly Sheet" to "मासिक रिपोर्ट / Monthly Sheet",
        "అమరికలు (Settings)" to "सेटिंग्स (Settings)",
        "భాషను ఎంచుకోండి" to "भाषा चुनें",
        "అప్‌డేట్" to "अपडेट",
        "అప్‌డేట్ అందుబాటులో ఉంది!" to "अपडेट उपलब्ध है!",
        "దయచేసి మీ ప్రాధాన్యత భాషను ఎంచుకోండి.\nPlease select your preferred language." to "कृपया अपनी पसंदीदा भाषा चुनें।\nPlease select your preferred language.",
        "కొనసాగించు / Continue" to "जारी रखें / Continue",
        "కొనసాగించు" to "जारी रखें",
        "కొనసాగించు (Continue)" to "जारी रखें (Continue)",
        "Continue (కొనసాగించు)" to "Continue (जारी रखें)",
        "భాష విజయవంతంగా తెలుగుకు సెట్ చేయబడింది!" to "भाषा सफलतापूर्वक तेलुगु में सेट हो गई है!",
        "భాష విజయవంతంగా ఇంగ్లీష్‌కు సెట్ చేయబడింది!" to "भाषा सफलतापूर्वक अंग्रेजी में सेट हो गई है!",
        "భద్రతా పిన్ సృష్టించండి" to "सुरक्षित पिन बनाएं",
        "సెక్యూరిటీ పిన్ సెట్ చేయడం ద్వారా మీ డేటా సురక్షितంగా ఉంటుంది." to "सुरक्षा पिन सेट करके अपना डेटा सुरक्षित करें।",
        "పిన్ నమోదు చేయండి" to "पिन दर्ज करें",
        "మనవాహనకు స్వాగతం" to "मानवाहन में आपका स्वागत है",
        "మన వాహనం - మన బాధ్యత." to "हमारी गाड़ी - हमारी जिम्मेदारी।",
        "నమోదు చేయండి" to "दर्ज करें",
        "తదుపరి" to "अगला",
        "వెనుకకు" to "पीछे",
        "పూర్తి చేయి" to "पूरा करें",
        "प्रారంభించు" to "शुरू करें",
        "డాక్యుమెంట్ వాల్ట్" to "दस्तावेज़ तिजोरी",
        "ఆఫ్‌లైన్ డాక్యుమెంట్లు" to "ऑफ़लाइन दस्तावेज़",
        "ఇంధన రికార్డులు" to "ईंधन रिकॉर्ड",
        "ఖర్చుల రికార్డులు" to "खर्च रिकॉर्ड",
        "సర్వీస్ హిస్టరీ" to "सेवा इतिहास",
        "సర్వీస్ రికార్డ్స్" to "सेवा रिकॉर्ड",
        "మొత్తం ఖర్చులు" to "कुल खर्च",
        "అన్ని లాగ్‌లు" to "सभी लॉग",
        "తేదీ" to "तारीख",
        "ధర" to "लागत",
        "రిటైల్ స్టేషన్" to "रिटेल स्टेशन",
        "లీటర్లు" to "लीटर",
        "రసీదు చిత్రం" to "रसीद चित्र",
        "ఫోటో తీయండి" to "फोटो लें",
        "గ్యాలరీ నుండి ఎంచుకోండి" to "गैलरी से चुनें",
        "సేవ్ చేయి" to "सहेजें",
        "రద్దు చేయి" to "रद्द करें",
        "తొలగించు" to "हटाएं",
        "సవరించు" to "संशोधित करें",
        "వివరాలు" to "विवरण",
        "గమనికలు" to "टिप्पणियां",
        "చిరునామా" to "पता",
        "యాప్ అప్‌డేట్‌లు" to "ऐप अपडेट",
        "సహాయం & మద్దతు" to "सहायता और सहायता",
        "వాహనం నంబర్" to "वाहन संख्या",
        "వాహనం పేరు" to "वाहन का नाम",
        "ఇంధన టైప్" to "ईंधन का प्रकार",
        "లక్షణాలు" to "विशेषताएं",
        "ఓడోమీటర్" to "ओडोमीटर",
        "గురించి" to "के बारे में",
        "సిస్టమ్ థీమ్" to "सिस्टम थीम",
        "డార్క్ థీమ్" to "डार्क थीम",
        "లైట్ థీమ్" to "लाइट थीम",
        "భాషను విజయవంతంగా అప్‌డేట్ చేసాం!" to "भाषा सफलतापूर्वक अपडेट की गई!"
    )

    private val teluguSubstringsToHindiMap = mapOf(
        "యజమాని" to "मालिक",
        "నమస్కారం" to "नमस्ते",
        "అమరికలు" to "सेटिंग्स",
        "మైలేజ్ (Mileage)" to "माइलेज (Mileage)",
        "మైలేజ్" to "माइलेज",
        "కొనసాగించు" to "जारी रखें",
        "తర్వాత" to "बाद में",
        "ఇప్పుడే" to "अभी",
        "అప్‌డేట్" to "अपडेट",
        "సేవ్ చేయి" to "सहेजें",
        "రద్దు చేయి" to "रद्द करें",
        "తేదీ" to "तारीख",
        "త్వరలో ముగుస్తుంది" to "जल्द समाप्त होगा",
        "గడువు ముగిసింది!" to "समाप्त!",
        "త్వరలో ముగియనుంది" to "जल्द समाप्त होने वाला है",
        "గడువు ముగిసింది" to "समाप्त",
        "పొల్యూషన్ సర్టిఫिकేట్ అలర్ట్" to "प्रदूषण प्रमाणपत्र चेतावनी",
        "పొల్యూషన్ గడువు ముగిసింది!" to "प्रदूषण की समय सीमा समाप्त!",
        "ఇన్సూరెన్స్ త్వరలో ముగియనుంది" to "बीमा जल्द समाप्त होने वाला है",
        "ఇన్సూరెన్స్ గడువు ముగిసింది!" to "बीमा समाप्त हो गया!",
        "మీ డాక్యుమెంట్ గడువు" to "आपका दस्तावेज़ समाप्त होने वाला है",
        "తేదీతో ముగియనుంది. ఇప్పుడే రిన్యూ చేసుకోండి!" to "को समाप्त हो जाएगा। अभी रिन्यू करें!",
        "తేదీతో ముగిసింది. దయచేసి వెంటనే రిన్యూ చేయండి!" to "को समाप्त हो गया। कृपया तुरंत रिन्यू करें!",
        "తో ముగియనుంది. తనిఖీ చేయించుకోండి!" to "को समाप्त हो जाएगा। कृपया जांच लें!",
        "నాటికి ముగిసింది. వెంటనే కొత్తది పొందండి!" to "को समाप्त हो गया। कृपया तुरंत नया प्राप्त करें!",
        "అలర్ట్" to "चेतावनी",
        "త్వరలో ఉంది/Upcoming" to "आगामी",
        "గడువు ముగిసింది/Overdue" to "समय सीमा पार",
        "త్వరలో ఉంది" to "आगामी",
        "బియాండ్" to "परे",
        "త్వరలో ముగుస్తుంది" to "जल्द समाप्त",
        "మీ సేవ్ చేసిన డాక్యుమెంట్" to "आपका सहेजा हुआ दस्तावेज़",
        "తీదీతో ముగిసింది. దయచేసి అప్‌డేట్ చేయండి!" to "को समाप्त हो गया। कृपया अपडेट करें!",
        "తేదీతో ముగిసింది. దయచేసి అప్‌డేట్ చేయండి!" to "को समाप्त हो गया। कृपया अपडेट करें!"
    )

    fun get(key: String, langCode: String?): String {
        val code = when (langCode) {
            "te" -> "te"
            "hi" -> "hi"
            else -> "en"
        }
        return translations[code]?.get(key) ?: translations["en"]?.get(key) ?: key
    }

    fun translate(text: String, langCode: String?): String {
        val code = langCode ?: "en"
        if (code == "en") {
            val trimmed = text.trim()
            val mapped = exactTeluguToEnglish[trimmed]
            if (mapped != null) return mapped

            // Handle substring translations
            var result = text
            teluguSubstringsMap.forEach { (te, en) ->
                if (result.contains(te)) {
                    result = result.replace(te, en)
                }
            }
            return result
        } else if (code == "hi") {
            val trimmed = text.trim()
            val mapped = exactTeluguToHindi[trimmed]
            if (mapped != null) return mapped

            // Handle substring translations
            var result = text
            teluguSubstringsToHindiMap.forEach { (te, hi) ->
                if (result.contains(te)) {
                    result = result.replace(te, hi)
                }
            }
            return result
        }
        return text
    }
}
