package com.erosketarakoa.app.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erosketarakoa.app.data.ShoppingRepository
import com.erosketarakoa.app.data.local.ListEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListsUiState(
    val lists: List<ListEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val repository: ShoppingRepository,
) : ViewModel() {

    /** Optimistic order held during/after a drag until the DB flow catches up. Null = use DB order. */
    private val pendingOrder = MutableStateFlow<List<ListEntity>?>(null)

    val uiState: StateFlow<ListsUiState> =
        combine(repository.observeLists(), pendingOrder) { lists, pending ->
            // If the DB now matches our optimistic order, drop the override.
            if (pending != null && pending.map { it.id } == lists.map { it.id }) {
                pendingOrder.value = null
            }
            ListsUiState(lists = pending ?: lists, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ListsUiState(),
        )

    /** Optimistically move a list during a drag. Persist with [commitReorder] on drop. */
    fun moveList(fromIndex: Int, toIndex: Int) {
        val current = pendingOrder.value ?: uiState.value.lists
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        pendingOrder.value = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }

    /** Persist the current optimistic order to the database. */
    fun commitReorder() {
        val order = pendingOrder.value ?: return
        viewModelScope.launch { repository.reorderLists(order.map { it.id }) }
    }

    fun createList(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createList(name) }
    }

    fun renameList(id: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.renameList(id, name) }
    }

    fun deleteList(id: String) {
        viewModelScope.launch { repository.deleteList(id) }
    }
}
