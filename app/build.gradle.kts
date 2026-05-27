plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

fun valoraeConfigValue(key: String, fallback: String = ""): String {
  val fromProject = (project.findProperty(key) as? String)?.trim().orEmpty()
  if (fromProject.isNotBlank()) return fromProject

  val fromEnv = System.getenv(key)?.trim().orEmpty()
  if (fromEnv.isNotBlank()) return fromEnv

  val envFiles = listOf(rootProject.file(".env"), rootProject.file(".env.example"))
  for (file in envFiles) {
    if (!file.exists()) continue
    val line = file.readLines().firstOrNull { raw ->
      val trimmed = raw.trim()
      trimmed.startsWith("$key=") && !trimmed.startsWith("#")
    }
    val value = line?.substringAfter('=')?.trim()?.trim('\"', '\'')
    if (!value.isNullOrBlank()) return value
  }

  return fallback
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.valorae.nbqpyl"
    minSdk = 24
    targetSdk = 36
    versionCode = 7
    versionName = "1.1.4"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Custom configurations exposed via BuildConfig.
    // Prioridade: gradle.properties > variáveis do ambiente/Studio > .env > .env.example > URL pública atual.
    val fallbackProxyUrl = "https://servidor-valorae.vercel.app"
    val valoraeUrl = valoraeConfigValue(
      "VALORAE_PROXY_BASE_URL",
      valoraeConfigValue("VERCEL_BACKEND_URL", fallbackProxyUrl)
    ).trimEnd('/')
    val valoraeClientId = valoraeConfigValue("VALORAE_PROXY_CLIENT_ID", "valorae-investidor-portfolio")
    val valoraeFallbackEnabled = valoraeConfigValue("VALORAE_DIRECT_FALLBACK_ENABLED", "false")

    buildConfigField("String", "VALORAE_PROXY_BASE_URL", "\"$valoraeUrl\"")
    buildConfigField("String", "VALORAE_PROXY_CLIENT_ID", "\"$valoraeClientId\"")
    buildConfigField("String", "VALORAE_DIRECT_FALLBACK_ENABLED", "\"$valoraeFallbackEnabled\"")
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("VALORAE_PROXY_BASE_URL")
  ignoreList.add("VALORAE_PROXY_CLIENT_ID")
  ignoreList.add("VALORAE_DIRECT_FALLBACK_ENABLED")
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.androidx.biometric)
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
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation(libs.jsoup)
  implementation(libs.vico.compose)
  implementation(libs.vico.compose.m3)
  implementation(libs.vico.core)
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
