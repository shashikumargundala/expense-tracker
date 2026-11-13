package com.shashi.expensetracker.settings

/**
 * State of [SettingsScreen]. [loaded] stays false until the stored session first emits, so the
 * screen shows only its title and never flashes wrong data. [email] is the signed-in account (null
 * when no session is stored) and [sheetId] its Expense Sheet (null hides the Sheet row).
 */
data class SettingsUiState(
  val loaded: Boolean = false,
  val email: String? = null,
  val sheetId: String? = null,
) {
  /** The avatar's letter: the email's first character, uppercased. */
  val initial: String
    get() = email?.firstOrNull()?.uppercase().orEmpty()
}
