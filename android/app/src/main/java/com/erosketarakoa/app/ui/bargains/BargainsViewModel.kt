package com.erosketarakoa.app.ui.bargains

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erosketarakoa.app.data.ShoppingRepository
import com.erosketarakoa.app.data.bargain.ItemBargains
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BargainsUiState(
    val bargains: List<ItemBargains> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class BargainsViewModel @Inject constructor(
    private val repository: ShoppingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BargainsUiState())
    val uiState: StateFlow<BargainsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = BargainsUiState(bargains = repository.computeBargains(), isLoading = false)
        }
    }
}
