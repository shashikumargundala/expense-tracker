// Top-level build file. Plugins are declared here and applied in each module.
plugins {
  base
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.hilt) apply false
}

// AD-17: no .kt file in any module may exceed 500 lines. Wired into `check`, which CI runs.
val maxKotlinFileLines = 500
val checkKotlinFileLength =
  tasks.register("checkKotlinFileLength") {
    group = "verification"
    description = "Fails if any .kt file has more than $maxKotlinFileLines lines."
    // Only module sources: never worktrees, tooling folders or build output under the root.
    val kotlinFiles =
      files(subprojects.map { sub -> fileTree(sub.file("src")) { include("**/*.kt") } }).asFileTree
    val root = rootDir
    val limit = maxKotlinFileLines
    inputs.files(kotlinFiles).withPropertyName("kotlinFiles")
    doLast {
      val tooLong =
        kotlinFiles.files
          .map { it to it.useLines { lines -> lines.count() } }
          .filter { (_, lines) -> lines > limit }
          .sortedBy { (file, _) -> file.path }
      if (tooLong.isNotEmpty()) {
        val report = tooLong.joinToString("\n") { (file, lines) -> "  ${file.relativeTo(root)}: $lines lines" }
        throw GradleException("Kotlin files over $limit lines (AD-17):\n$report")
      }
    }
  }

tasks.named("check") { dependsOn(checkKotlinFileLength) }
