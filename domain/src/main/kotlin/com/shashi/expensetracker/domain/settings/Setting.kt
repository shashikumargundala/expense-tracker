package com.shashi.expensetracker.domain.settings

/** One Settings row (AD-7, AD-20), keyed by [key]. [value] may be blank. */
data class Setting(val key: String, val value: String)
