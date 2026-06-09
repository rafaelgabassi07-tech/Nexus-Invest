import java.util.Base64

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


fun safeValoraeProxyUrl(raw: String, fallback: String): String {
  val value = raw.trim().trimEnd('/')
  val lower = value.lowercase()
  return if (
    value.isBlank() ||
    !value.startsWith("https://") ||
    lower.contains("valorae-proxy.vercel.app") ||
    lower.contains("your-backend") ||
    lower.contains("seu-dominio") ||
    lower.contains("localhost") ||
    lower.contains("10.0.2.2") ||
    lower.contains("127.0.0.1")
  ) {
    fallback
  } else {
    value
  }
}

fun ensureDebugKeystore() {
  val keystoreFile = rootProject.file("debug.keystore")
  val base64File = rootProject.file("debug.keystore.base64")
  if (!keystoreFile.exists() && base64File.exists()) {
    try {
      val bytes = Base64.getDecoder().decode(base64File.readText().trim())
      keystoreFile.writeBytes(bytes)
    } catch (e: Exception) {
      // Ignorar caso falhe
    }
  }
}
ensureDebugKeystore()

fun hasReleaseSigningMaterial(): Boolean {
  val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
  return file(keystorePath).exists() && !System.getenv("STORE_PASSWORD").isNullOrBlank()
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.valorae.nbqpyl"
    minSdk = 24
    targetSdk = 36
    versionCode = 50
    versionName = "2.0.40"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Custom configurations exposed via BuildConfig.
    // Prioridade: gradle.properties > variáveis do ambiente/Studio > .env > .env.example > URL pública atual.
    val fallbackProxyUrl = "https://servidor-valorae.vercel.app"
    val valoraeUrl = safeValoraeProxyUrl(
      valoraeConfigValue(
        "VALORAE_API_BASE_URL",
        valoraeConfigValue(
          "VALORAE_PROXY_BASE_URL",
          valoraeConfigValue("VALORAE_PUBLIC_BASE_URL", valoraeConfigValue("VERCEL_BACKEND_URL", fallbackProxyUrl))
        )
      ),
      fallbackProxyUrl
    )
    val valoraeClientId = valoraeConfigValue("VALORAE_PROXY_CLIENT_ID", "valorae-investidor-android")
    val valoraeFallbackEnabled = valoraeConfigValue("VALORAE_DIRECT_FALLBACK_ENABLED", "false")

    buildConfigField("String", "VALORAE_API_BASE_URL", "\"$valoraeUrl\"")
    buildConfigField("String", "VALORAE_PROXY_BASE_URL", "\"$valoraeUrl\"")
    buildConfigField("String", "VALORAE_PUBLIC_BASE_URL", "\"$valoraeUrl\"")
    buildConfigField("String", "VALORAE_PROXY_CLIENT_ID", "\"$valoraeClientId\"")
    buildConfigField("String", "VALORAE_DIRECT_FALLBACK_ENABLED", "\"$valoraeFallbackEnabled\"")
    buildConfigField("String", "VALORAE_UPDATE_MANIFEST_URL", "\"https://app-atualizacoes.vercel.app/update.json\"")
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
      signingConfig = if (hasReleaseSigningMaterial()) signingConfigs.getByName("release") else signingConfigs.getByName("debugConfig")
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
  ignoreList.add("VALORAE_API_BASE_URL")
  ignoreList.add("VALORAE_PROXY_BASE_URL")
  ignoreList.add("VALORAE_PUBLIC_BASE_URL")
  ignoreList.add("VALORAE_PROXY_CLIENT_ID")
  ignoreList.add("VALORAE_DIRECT_FALLBACK_ENABLED")
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom)) // Não usado; manter removido para reduzir sync/build e APK.
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.profileinstaller)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  // implementation(libs.jsoup) // Não usado no app; scraping direto no Android não deve ser feito.
  // implementation(libs.vico.compose) // Gráficos atuais usam componentes Compose internos.
  // implementation(libs.vico.compose.m3) // Gráficos atuais usam componentes Compose internos.
  // implementation(libs.vico.core) // Gráficos atuais usam componentes Compose internos.
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
