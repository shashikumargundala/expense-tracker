package com.shashi.expensetracker.domain.category

/**
 * The unique key of a Category name: trimmed, with ASCII letters A–Z folded to lowercase (AD-9).
 * Other characters stay as they are, so the key matches the mirror's SQLite `COLLATE NOCASE`
 * exactly: `Café` and `CAFÉ` are two Categories.
 */
object CategoryKey {
  fun of(name: String): String =
    buildString(name.length) { name.trim().forEach { c -> append(if (c in 'A'..'Z') c + ('a' - 'A') else c) } }
}
