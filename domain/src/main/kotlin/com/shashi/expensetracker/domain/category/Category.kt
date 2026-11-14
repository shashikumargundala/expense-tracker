package com.shashi.expensetracker.domain.category

/**
 * A Category from the Sheet's Categories tab (AD-7). It is keyed by [name], trimmed and
 * case-folded (AD-9). [color] is a `#RRGGBB` string and [icon] a Material Symbols name; either may
 * be blank for a hand-typed row.
 */
data class Category(val name: String, val color: String, val icon: String)
