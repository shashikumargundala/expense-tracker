package com.shashi.expensetracker.data.sync

import com.shashi.expensetracker.domain.account.ExpenseRowFix
import com.shashi.expensetracker.domain.account.ExpenseRowRead
import com.shashi.expensetracker.domain.account.SheetRow
import com.shashi.expensetracker.domain.category.CategoryKey
import com.shashi.expensetracker.domain.expense.Expense
import java.util.UUID

/**
 * AD-5's pull-time ID repair of the Expenses tab, as a pure function of one read. An invalid row
 * (AD-7) is never fixed or deleted, but it holds its place in its ID's sheet order.
 * - A row with a blank ID gets a new UUID.
 * - Among rows sharing an ID, a later valid row whose mapped values equal any earlier valid row's
 *   is a retry duplicate and is deleted. Of the rows left, the first in sheet order keeps the ID;
 *   each later valid one gets a new UUID and its SMS ID cleared. So when the first row is invalid,
 *   every distinct valid row after it gets a new UUID.
 * - Every row that stays and has a blank Source gets `User` written to it.
 *
 * "Equal" compares Date, Amount, Category by [CategoryKey], Description, Source (a blank one read
 * as `User`) and SMS ID.
 */
internal object ExpenseIdRepair {

  /** What to write back: [fixes] first, then [deletes] (1-based rows of the same read). */
  data class Plan(val fixes: List<ExpenseRowFix>, val deletes: List<Int>) {
    /** Whether a row moves or gets a new ID, so the read no longer matches the Sheet by ID. */
    val changesIds: Boolean
      get() = deletes.isNotEmpty() || fixes.any { it.id != null }

    val isEmpty: Boolean
      get() = fixes.isEmpty() && deletes.isEmpty()
  }

  fun plan(rows: List<SheetRow<ExpenseRowRead>>, newId: () -> String = { UUID.randomUUID().toString() }): Plan {
    val fixes = mutableListOf<ExpenseRowFix>()
    val deletes = mutableListOf<Int>()
    // ID to the normalized values of its valid rows so far; an invalid first row leaves it empty.
    val groups = HashMap<String, MutableSet<Expense>>()
    for ((row, read) in rows) {
      val id = read.id
      val expense = read.expense
      if (expense == null) {
        if (id != null) groups.getOrPut(id) { HashSet() }
        continue
      }
      if (id == null) {
        fixes += ExpenseRowFix(row, id = newId(), sourceUser = read.sourceBlank)
        continue
      }
      val seen = groups[id]
      val values = expense.normalized()
      when {
        seen == null -> {
          groups[id] = hashSetOf(values)
          if (read.sourceBlank) fixes += ExpenseRowFix(row, sourceUser = true)
        }
        values in seen -> deletes += row
        else -> {
          seen += values
          fixes += ExpenseRowFix(row, id = newId(), sourceUser = read.sourceBlank, clearSmsId = expense.smsId != null)
        }
      }
    }
    return Plan(fixes, deletes)
  }

  private fun Expense.normalized(): Expense = copy(category = CategoryKey.of(category))
}
