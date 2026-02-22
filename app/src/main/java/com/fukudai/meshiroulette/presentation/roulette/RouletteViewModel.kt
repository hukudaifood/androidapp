package com.fukudai.meshiroulette.presentation.roulette

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.domain.model.PriceRange
import com.fukudai.meshiroulette.domain.usecase.GetRestaurantsUseCase
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
    private val spinRouletteUseCase: SpinRouletteUseCase,
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouletteUiState())
    val uiState: StateFlow<RouletteUiState> = _uiState.asStateFlow()

    init {
        loadCandidates()
    }

    fun spinRoulette() {
        viewModelScope.launch {
            spinRouletteUseCase(
                genres = _uiState.value.selectedGenres.toList().takeIf { it.isNotEmpty() },
                priceRange = _uiState.value.selectedPriceRange,
                isOpenNow = _uiState.value.isOpenNowOnly.takeIf { it }
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

    private fun loadCandidates() {
        viewModelScope.launch {
            getRestaurantsUseCase(
                genres = _uiState.value.selectedGenres.toList().takeIf { it.isNotEmpty() },
                priceRange = _uiState.value.selectedPriceRange.takeIf { it != PriceRange.ALL },
                isOpenNow = _uiState.value.isOpenNowOnly.takeIf { it }
            ).collect { result ->
                if (result is NetworkResult.Success) {
                    _uiState.update { it.copy(candidateRestaurants = result.data) }
                }
            }
        }
    }

    fun toggleGenre(genre: Genre) {
        _uiState.update { state ->
            val newGenres = if (genre == Genre.ALL) {
                emptySet()
            } else {
                if (genre in state.selectedGenres) {
                    state.selectedGenres - genre
                } else {
                    state.selectedGenres + genre
                }
            }
            state.copy(selectedGenres = newGenres)
        }
        loadCandidates()
    }

    fun setPriceRange(priceRange: PriceRange) {
        _uiState.update { it.copy(selectedPriceRange = priceRange) }
        loadCandidates()
    }

    fun setOpenNowOnly(isOpenNow: Boolean) {
        _uiState.update { it.copy(isOpenNowOnly = isOpenNow) }
        loadCandidates()
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
