import java.io.File

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.manavahana"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.Lochan.ManaVahana"
    minSdk = 24
    targetSdk = 36
    versionCode = 32
    versionName = "3.4.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    val releaseKeystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/ManaVahna_key.jks"
    val releaseKeystoreFile = file(releaseKeystorePath)
    if (releaseKeystoreFile.exists()) {
      create("release") {
        storeFile = releaseKeystoreFile
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "ManaVahnaKey"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
    if (file("${rootDir}/debug.keystore").exists()) {
      create("debugConfig") {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val releaseConfig = signingConfigs.findByName("release")
      if (releaseConfig != null) {
        signingConfig = releaseConfig
      }
    }
    debug {
      val customDebugConfig = signingConfigs.findByName("debugConfig")
      if (customDebugConfig != null) {
        signingConfig = customDebugConfig
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  packaging {
    jniLibs {
      // Ensure native shared libraries (.so) are packaged uncompressed and 16 KB (16384 bytes) page-aligned
      // as required by Android 15+ (API 35+) and Google Play policy.
      useLegacyPackaging = false
    }
    resources {
      excludes += listOf(
        "/META-INF/{AL2.0,LGPL2.1}",
        "/META-INF/INDEX.LIST",
        "/META-INF/DEPENDENCIES"
      )
    }
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

val appBuildDir = layout.buildDirectory

// Verification task to ensure all 64-bit native libraries (.so) have 16 KB (16384 bytes) ELF alignment
tasks.register("verify16KbAlignment") {
  description = "Verifies that all 64-bit native libraries (.so) in the build are aligned to 16 KB page boundaries (16384 bytes)."
  group = "verification"
  val targetBuildDir = appBuildDir

  doLast {
    val bDir = targetBuildDir.get().asFile
    val searchDirs = listOf(
      File(bDir, "intermediates/merged_native_libs"),
      File(bDir, "intermediates/stripped_native_libs")
    )
    var checkedCount = 0
    val violations = mutableListOf<String>()

    for (dir in searchDirs) {
      if (!dir.exists()) continue
      dir.walkTopDown().filter { it.isFile && it.extension == "so" }.forEach { soFile ->
        val path = soFile.invariantSeparatorsPath
        if (path.contains("arm64-v8a") || path.contains("x86_64")) {
          val bytes = soFile.readBytes()
          if (bytes.size >= 64 && bytes[0] == 0x7f.toByte() && bytes[1] == 'E'.code.toByte() && bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
            val is64Bit = bytes[4] == 2.toByte()
            if (is64Bit) {
              checkedCount++
              // Read little-endian 64-bit e_phoff at byte 32
              var ePhoOff = 0L
              for (j in 0 until 8) {
                ePhoOff = ePhoOff or ((bytes[32 + j].toLong() and 0xFFL) shl (j * 8))
              }
              val ePhOffInt = ePhoOff.toInt()

              // Read little-endian 16-bit e_phentsize at byte 54
              val ePhEntSize = (bytes[54].toInt() and 0xFF) or ((bytes[55].toInt() and 0xFF) shl 8)
              // Read little-endian 16-bit e_phnum at byte 56
              val ePhNum = (bytes[56].toInt() and 0xFF) or ((bytes[57].toInt() and 0xFF) shl 8)

              var hasLoadSegment = false
              var isAligned = true
              for (i in 0 until ePhNum) {
                val offset = ePhOffInt + i * ePhEntSize
                if (offset + 56 <= bytes.size) {
                  // Read 32-bit p_type at offset
                  val pType = (bytes[offset].toInt() and 0xFF) or
                          ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                          ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                          ((bytes[offset + 3].toInt() and 0xFF) shl 24)

                  if (pType == 1) { // PT_LOAD segment
                    hasLoadSegment = true
                    var pAlign = 0L
                    for (j in 0 until 8) {
                      pAlign = pAlign or ((bytes[offset + 48 + j].toLong() and 0xFFL) shl (j * 8))
                    }
                    if (pAlign < 16384L) {
                      isAligned = false
                      violations.add("${soFile.name} in $path has PT_LOAD alignment of $pAlign bytes (< 16384)")
                    }
                  }
                }
              }
              if (hasLoadSegment && isAligned) {
                println("✓ Verified 16 KB alignment for: ${soFile.name} (${if (path.contains("arm64-v8a")) "arm64-v8a" else "x86_64"})")
              }
            }
          }
        }
      }
    }

    println("16 KB page size verification completed: $checkedCount 64-bit native libraries inspected.")
    if (violations.isNotEmpty()) {
      throw GradleException("16 KB page size alignment verification failed:\n" + violations.joinToString("\n"))
    } else {
      println("SUCCESS: All checked 64-bit native libraries comply with 16 KB memory page sizes.")
    }
  }
}

tasks.matching { it.name == "mergeReleaseNativeLibs" || it.name == "mergeDebugNativeLibs" }.configureEach {
  finalizedBy("verify16KbAlignment")
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation("androidx.graphics:graphics-path:1.1.0")
  implementation(libs.coil.compose)
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation(libs.play.app.update)
  implementation(libs.play.app.update.ktx)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
