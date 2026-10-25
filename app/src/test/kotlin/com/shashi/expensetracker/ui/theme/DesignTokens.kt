package com.shashi.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color
import java.io.File

/** Reads color tokens straight from DESIGN.md's front matter, so the theme is checked at the source. */
object DesignTokens {

  private const val DESIGN_MD =
    "_bmad-output/initiative-expense-tracker/ux-expense-tracker/DESIGN.md"

  /** Finds DESIGN.md from the test working directory (the `:app` project dir) or any parent. */
  fun designFile(): File {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
      val candidate = File(dir, DESIGN_MD)
      if (candidate.isFile) return candidate
      dir = dir.parentFile
    }
    error("DESIGN.md not found at ../$DESIGN_MD")
  }

  /** Every `colors:` entry in the front matter, in file order: token name to hex string. */
  fun colorTokens(file: File = designFile()): Map<String, String> {
    val lines = file.readLines()
    check(lines.firstOrNull() == "---") { "DESIGN.md has no front matter" }
    val end = lines.drop(1).indexOf("---") + 1
    check(end > 0) { "DESIGN.md front matter is not closed" }
    val frontMatter = lines.subList(1, end)
    val start = frontMatter.indexOf("colors:")
    check(start >= 0) { "DESIGN.md front matter has no colors block" }
    val entry = Regex("""^ {2}([a-z0-9-]+):\s*'(#[0-9A-Fa-f]{6}(?:[0-9A-Fa-f]{2})?)'\s*$""")
    val tokens = LinkedHashMap<String, String>()
    for (line in frontMatter.drop(start + 1)) {
      if (line.isNotEmpty() && !line.startsWith(" ")) break // next top-level key
      val match = entry.find(line) ?: continue
      tokens[match.groupValues[1]] = match.groupValues[2]
    }
    return tokens
  }

  /** Converts DESIGN.md's `#RRGGBB` or `#RRGGBBAA` (alpha last) to a Compose `Color`. */
  fun parseDesignHex(hex: String): Color {
    val digits = hex.removePrefix("#")
    require(digits.length == 6 || digits.length == 8) { "Bad hex $hex" }
    val rgb = digits.substring(0, 6).toLong(16)
    val alpha = if (digits.length == 8) digits.substring(6, 8).toLong(16) else 0xFFL
    return Color((alpha shl 24) or rgb)
  }
}
