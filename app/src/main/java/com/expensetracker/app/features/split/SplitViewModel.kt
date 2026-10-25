package com.expensetracker.app.features.split

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.GroupDetailSummary
import com.expensetracker.app.core.data.repository.SplitRepository
import com.expensetracker.app.core.database.entity.GroupEntity
import com.expensetracker.app.core.database.entity.GroupSettlementEntity
import com.expensetracker.app.core.database.entity.SplitExpenseEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplitUiState(
    val groups: List<GroupEntity> = emptyList(),
    val selectedGroupId: Long? = null,
    val selectedGroupSummary: GroupDetailSummary? = null,
    val selectedGroupExpenses: List<SplitExpenseEntity> = emptyList(),
    val selectedGroupSettlements: List<GroupSettlementEntity> = emptyList()
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SplitViewModel @Inject constructor(
    private val splitRepository: SplitRepository
) : ViewModel() {

    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<SplitUiState> = combine(
        splitRepository.allGroups,
        _selectedGroupId,
        _refreshTrigger
    ) { groups, selectedId, _ ->
        val summary = if (selectedId != null) splitRepository.getGroupSummary(selectedId) else null
        Triple(groups, selectedId, summary)
    }.flatMapLatest { (groups, selectedId, summary) ->
        val expensesFlow = if (selectedId != null) splitRepository.getExpensesByGroup(selectedId) else flowOf(emptyList())
        val settlementsFlow = if (selectedId != null) splitRepository.getSettlementsByGroup(selectedId) else flowOf(emptyList())

        combine(expensesFlow, settlementsFlow) { expList, settList ->
            SplitUiState(
                groups = groups,
                selectedGroupId = selectedId,
                selectedGroupSummary = summary,
                selectedGroupExpenses = expList,
                selectedGroupSettlements = settList
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SplitUiState()
    )

    fun selectGroup(groupId: Long?) {
        _selectedGroupId.value = groupId
        refreshSummary()
    }

    fun createGroup(name: String, description: String, emoji: String, memberNames: List<String>) {
        viewModelScope.launch {
            val id = splitRepository.createGroup(name, description, emoji, memberNames)
            _selectedGroupId.value = id
            refreshSummary()
        }
    }

    fun deleteGroup(group: GroupEntity) {
        viewModelScope.launch {
            splitRepository.deleteGroup(group)
            if (_selectedGroupId.value == group.id) {
                _selectedGroupId.value = null
            }
            refreshSummary()
        }
    }

    fun addExpense(
        groupId: Long,
        title: String,
        amount: Double,
        paidBy: String,
        category: String,
        customShares: Map<String, Double>? = null
    ) {
        viewModelScope.launch {
            splitRepository.addExpense(groupId, title, amount, paidBy, category, customShares)
            refreshSummary()
        }
    }

    fun deleteExpense(expense: SplitExpenseEntity) {
        viewModelScope.launch {
            splitRepository.deleteExpense(expense)
            refreshSummary()
        }
    }

    fun recordSettlement(groupId: Long, fromMember: String, toMember: String, amount: Double) {
        viewModelScope.launch {
            splitRepository.recordSettlement(groupId, fromMember, toMember, amount)
            refreshSummary()
        }
    }

    fun copyWhatsAppSummaryToClipboard(context: Context): Boolean {
        val summary = uiState.value.selectedGroupSummary ?: return false
        val text = splitRepository.generateWhatsAppSummaryText(summary)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("Group Settlement Summary", text)
            clipboard.setPrimaryClip(clip)
            return true
        }
        return false
    }

    private fun refreshSummary() {
        _refreshTrigger.value = _refreshTrigger.value + 1
    }
}
