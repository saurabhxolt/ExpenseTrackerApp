package com.expensetracker.app.features.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class CategoryShare(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float
)

data class AnalyticsUiState(
    val categoryShares: List<CategoryShare> = emptyList(),
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalInvestments: Double = 0.0,
    val topSpendingCategory: String = "None",
    val monthYearOptions: List<String> = listOf("ALL"),
    val periodPresetOptions: List<String> = listOf("ALL", "THIS_MONTH", "LAST_MONTH", "LAST_3_MONTHS", "LAST_6_MONTHS", "YEARLY", "CUSTOM"),
    val selectedPeriodPreset: String = "ALL",
    val selectedMonthYear: String = "ALL",
    val customStartDateMs: Long? = null,
    val customEndDateMs: Long? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedPeriodPreset = MutableStateFlow("ALL")
    private val _selectedMonthYear = MutableStateFlow("ALL")
    private val _customStartDateMs = MutableStateFlow<Long?>(null)
    private val _customEndDateMs = MutableStateFlow<Long?>(null)

    private val _filterState = combine(
        _selectedPeriodPreset,
        _selectedMonthYear,
        _customStartDateMs,
        _customEndDateMs
    ) { preset, monthYear, startMs, endMs ->
        AnalyticsFilterState(preset, monthYear, startMs, endMs)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        transactionRepository.allTransactions,
        _filterState
    ) { transactions, filterState ->
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val availableMonths = transactions
            .map { monthFormat.format(Date(it.timestamp)) }
            .distinct()
            .sortedDescending()
        val allMonthOptions = listOf("ALL") + availableMonths

        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)

        val filtered = transactions.filter { item ->
            val matchesMonth = if (filterState.monthYear == "ALL") true else monthFormat.format(Date(item.timestamp)) == filterState.monthYear

            val itemCal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
            val itemYear = itemCal.get(Calendar.YEAR)
            val itemMonth = itemCal.get(Calendar.MONTH)

            val matchesPeriod = when (filterState.periodPreset) {
                "THIS_MONTH" -> itemYear == currentYear && itemMonth == currentMonth
                "LAST_MONTH" -> {
                    val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                    itemYear == lastMonthCal.get(Calendar.YEAR) && itemMonth == lastMonthCal.get(Calendar.MONTH)
                }
                "LAST_3_MONTHS" -> item.timestamp >= (System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000)
                "LAST_6_MONTHS" -> item.timestamp >= (System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000)
                "YEARLY" -> itemYear == currentYear
                "CUSTOM" -> {
                    val start = filterState.customStartMs ?: 0L
                    val end = filterState.customEndMs ?: Long.MAX_VALUE
                    item.timestamp in start..end
                }
                else -> true // ALL
            }

            matchesMonth && matchesPeriod
        }

        val debitTransactions = filtered.filter { it.type == "DEBIT" }
        val creditTransactions = filtered.filter { it.type == "CREDIT" }
        val investmentTransactions = filtered.filter { it.type == "INVESTMENT" }

        val totalExp = debitTransactions.sumOf { it.amount }
        val totalInc = creditTransactions.sumOf { it.amount }
        val totalInv = investmentTransactions.sumOf { it.amount }

        val shares = (debitTransactions + investmentTransactions)
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .map { (cat, amount) ->
                val base = totalExp + totalInv
                val pct = if (base > 0) (amount / base).toFloat() else 0f
                CategoryShare(categoryName = cat, totalAmount = amount, percentage = pct)
            }
            .sortedByDescending { it.totalAmount }

        val topCategory = shares.firstOrNull()?.categoryName ?: "None"

        AnalyticsUiState(
            categoryShares = shares,
            totalExpenses = totalExp,
            totalIncome = totalInc,
            totalInvestments = totalInv,
            topSpendingCategory = topCategory,
            monthYearOptions = allMonthOptions,
            selectedPeriodPreset = filterState.periodPreset,
            selectedMonthYear = filterState.monthYear,
            customStartDateMs = filterState.customStartMs,
            customEndDateMs = filterState.customEndMs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun onPeriodPresetSelected(preset: String) {
        _selectedPeriodPreset.value = preset
        if (preset != "CUSTOM") {
            _customStartDateMs.value = null
            _customEndDateMs.value = null
        }
    }

    fun onMonthYearSelected(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    fun onCustomDateRangeSelected(startMs: Long?, endMs: Long?) {
        _customStartDateMs.value = startMs
        _customEndDateMs.value = endMs
        _selectedPeriodPreset.value = "CUSTOM"
    }
}

private data class AnalyticsFilterState(
    val periodPreset: String,
    val monthYear: String,
    val customStartMs: Long?,
    val customEndMs: Long?
)
