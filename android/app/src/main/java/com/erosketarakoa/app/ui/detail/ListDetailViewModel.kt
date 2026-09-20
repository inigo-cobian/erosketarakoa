package com.erosketarakoa.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erosketarakoa.app.data.ShoppingRepository
import com.erosketarakoa.app.data.local.ItemEntity
import com.erosketarakoa.app.data.local.ListEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListDetailUiState(
    val list: ListEntity? = null,
    val items: List<ItemEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    private val repository: ShoppingRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val listId: String = checkNotNull(savedStateHandle["listId"]) { "listId is required" }

    /** Optimistic order held during/after a drag until the DB flow catches up. Null = use DB order. */
    private val pendingOrder = MutableStateFlow<List<ItemEntity>?>(null)

    val uiState: StateFlow<ListDetailUiState> =
        combine(
            repository.observeList(listId),
            repository.observeItems(listId),
            pendingOrder,
        ) { list, items, pending ->
            // If the DB now matches our optimistic order, drop the override.
            if (pending != null && pending.map { it.id } == items.map { it.id }) {
                pendingOrder.value = null
            }
            val shown = pending ?: items
            ListDetailUiState(list = list, items = shown, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ListDetailUiState(),
        )

    /** Optimistically move an item during a drag. Persist with [commitReorder] on drop. */
    fun moveItem(fromIndex: Int, toIndex: Int) {
        val current = pendingOrder.value ?: uiState.value.items
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        pendingOrder.value = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }

    /** Persist the current optimistic order to the database. */
    fun commitReorder() {
        val order = pendingOrder.value ?: return
        viewModelScope.launch { repository.reorderItems(order.map { it.id }) }
    }

    fun addItem(
        name: String,
        quantity: Int,
        unit: String?,
        supermarkets: List<String>,
        category: String?,
        notes: String?,
        icon: String,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addItem(listId, name, quantity, unit, supermarkets, category, notes, icon)
        }
    }

    fun updateItem(
        id: String,
        name: String,
        quantity: Int,
        unit: String?,
        supermarkets: List<String>,
        category: String?,
        notes: String?,
        icon: String,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.updateItem(id, name, quantity, unit, supermarkets, category, notes, icon)
        }
    }

    fun toggleBought(item: ItemEntity) {
        viewModelScope.launch { repository.setBought(item.id, !item.bought) }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch { repository.deleteItem(id) }
    }

    fun restoreItem(item: ItemEntity) {
        viewModelScope.launch { repository.restoreItem(item) }
    }
}
