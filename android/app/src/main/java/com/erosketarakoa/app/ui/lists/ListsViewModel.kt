package com.erosketarakoa.app.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erosketarakoa.app.data.ShoppingRepository
import com.erosketarakoa.app.data.local.ListEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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

    val uiState: StateFlow<ListsUiState> =
        repository.observeLists()
            .map { ListsUiState(lists = it, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ListsUiState(),
            )

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
