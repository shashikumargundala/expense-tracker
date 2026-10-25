plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
}

android {
  namespace = "com.shashi.expensetracker"
  compileSdk { version = release(37) }

  defaultConfig {
    applicationId = "com.shashi.expensetracker"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  }

  // Debug builds use AGP's default debug signing (~/.android/debug.keystore).
  // Release builds are signed from the environment. Without all four variables the release
  // stays unsigned, so builds without secrets (CI, local) still work. Never falls back to debug.
  val releaseSigningEnv =
    listOf("KEYSTORE_PATH", "KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD").associateWith {
      providers.environmentVariable(it).orNull
    }
  val missingSigningEnv = releaseSigningEnv.filterValues { it.isNullOrBlank() }.keys
  val releaseSigning =
    if (missingSigningEnv.isEmpty()) {
      signingConfigs.create("release") {
        storeFile = rootProject.file(releaseSigningEnv.getValue("KEYSTORE_PATH")!!)
        storePassword = releaseSigningEnv.getValue("KEYSTORE_PASSWORD")
        keyAlias = releaseSigningEnv.getValue("KEY_ALIAS")
        keyPassword = releaseSigningEnv.getValue("KEY_PASSWORD")
      }
    } else {
      logger.warn("Release builds will be unsigned: missing ${missingSigningEnv.joinToString()}.")
      null
    }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = releaseSigning
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { compose = true }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
  implementation(project(":domain"))
  // :data is here only so Hilt can install its bindings; app code imports :domain types only.
  implementation(project(":data"))

  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  testImplementation(platform(libs.androidx.compose.bom))
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.ext.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)

  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

// Robolectric's SDK 36 sandbox needs Java 21 and access to JDK FileDescriptor internals;
// only the test JVM uses this.
tasks.withType<Test>().configureEach {
  javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
  jvmArgs(
    "--add-opens=java.base/java.io=ALL-UNNAMED",
    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
  )
}
