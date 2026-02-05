package com.fukudai.meshiroulette.presentation.roulette

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.domain.model.PriceRange
import com.fukudai.meshiroulette.domain.usecase.SpinRouletteUseCase
import com.fukudai.meshiroulette.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouletteViewModel @Inject constructor(
    private val spinRouletteUseCase: SpinRouletteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouletteUiState())
    val uiState: StateFlow<RouletteUiState> = _uiState.asStateFlow()

    fun spinRoulette() {
        viewModelScope.launch {
            spinRouletteUseCase(
                genre = _uiState.value.selectedGenre,
                priceRange = _uiState.value.selectedPriceRange
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isSpinning = true, error = null) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isSpinning = false,
                                selectedRestaurant = result.data,
                                error = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(isSpinning = false, error = result.message)
                        }
                    }
                }
            }
        }
    }

    fun setGenre(genre: Genre) {
        _uiState.update { it.copy(selectedGenre = genre) }
    }

    fun setPriceRange(priceRange: PriceRange) {
        _uiState.update { it.copy(selectedPriceRange = priceRange) }
    }

    fun showFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = true) }
    }

    fun hideFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = false) }
    }

    fun clearResult() {
        _uiState.update { it.copy(selectedRestaurant = null, error = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
