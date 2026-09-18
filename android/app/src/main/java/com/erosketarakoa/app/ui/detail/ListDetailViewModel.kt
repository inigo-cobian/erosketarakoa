package com.erosketarakoa.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erosketarakoa.app.data.ShoppingRepository
import com.erosketarakoa.app.data.local.ItemEntity
import com.erosketarakoa.app.data.local.ListEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    val uiState: StateFlow<ListDetailUiState> =
        combine(
            repository.observeList(listId),
            repository.observeItems(listId),
        ) { list, items ->
            ListDetailUiState(list = list, items = items, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ListDetailUiState(),
        )

    fun addItem(name: String, quantity: Int, category: String?, notes: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addItem(listId, name, quantity, category, notes)
        }
    }

    fun updateItem(id: String, name: String, quantity: Int, category: String?, notes: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.updateItem(id, name, quantity, category, notes)
        }
    }

    fun toggleBought(item: ItemEntity) {
        viewModelScope.launch { repository.setBought(item.id, !item.bought) }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch { repository.deleteItem(id) }
    }
}
