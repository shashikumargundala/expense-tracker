package com.shashi.expensetracker.domain.budget

import java.time.YearMonth

/**
 * One Budgets row (AD-7, AD-8): the [amountPaise] (AD-6) set for [category] from [month] onward.
 * Keyed by ([month], [category]), with the Category name compared trimmed and case-folded (AD-9).
 */
data class Budget(val month: YearMonth, val category: String, val amountPaise: Long)
