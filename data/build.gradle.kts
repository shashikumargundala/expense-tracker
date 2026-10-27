plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
}

// The Web OAuth client ID from gradle.properties. Not a secret; blank fails sign-in with a named
// failure (docs/GOOGLE_CLOUD_SETUP.md).
val googleWebClientId = providers.gradleProperty("googleWebClientId").getOrElse("").trim()

require(googleWebClientId.none { it == '"' || it == '\\' }) {
  "googleWebClientId in gradle.properties must not contain quotes or backslashes."
}

android {
  namespace = "com.shashi.expensetracker.data"
  compileSdk { version = release(37) }

  defaultConfig { minSdk = 24 }

  // Per build type, so the release build can take its own value later (story 2.7).
  buildTypes {
    debug { buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"") }
    release { buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"") }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { buildConfig = true }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Room writes each schema version here; later epics add tables with migrations against it.
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
  implementation(project(":domain"))
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)

  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.ext.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.room.testing)
}

// Robolectric's SDK 36 sandbox needs Java 21; only the test JVM uses this (as in :app).
tasks.withType<Test>().configureEach {
  javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
  jvmArgs(
    "--add-opens=java.base/java.io=ALL-UNNAMED",
    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
  )
}
