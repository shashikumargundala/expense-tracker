package com.shashi.expensetracker.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shashi.expensetracker.domain.category.ObserveCategoriesUseCase
import com.shashi.expensetracker.domain.expense.AddExpenseResult
import com.shashi.expensetracker.domain.expense.AddExpenseUseCase
import com.shashi.expensetracker.domain.expense.ObserveExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Lists the mirror's Expenses and adds new ones; both go through `:domain` use cases only. */
@HiltViewModel
class ExpensesViewModel
@Inject
constructor(
  observeExpenses: ObserveExpensesUseCase,
  observeCategories: ObserveCategoriesUseCase,
  private val addExpense: AddExpenseUseCase,
) : ViewModel() {

  private val saveFailed = MutableStateFlow(false)

  val uiState: StateFlow<ExpensesUiState> =
    combine(observeExpenses(), observeCategories(), saveFailed) { expenses, categories, failed ->
        ExpensesUiState(
          loaded = true,
          expenses = expenses,
          categories = categories.map { it.name },
          saveFailed = failed,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ExpensesUiState())

  /** Adds an Expense dated now. A failure shows [ExpensesUiState.saveFailed]; nothing is written. */
  fun add(amountPaise: Long, category: String, description: String) {
    viewModelScope.launch {
      saveFailed.value = addExpense(amountPaise, category, description) is AddExpenseResult.Failure
    }
  }

  private companion object {
    const val STOP_TIMEOUT_MILLIS = 5_000L
  }
}
