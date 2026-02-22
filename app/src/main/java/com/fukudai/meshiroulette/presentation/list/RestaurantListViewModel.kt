package com.fukudai.meshiroulette.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.domain.model.PriceRange
import com.fukudai.meshiroulette.domain.usecase.GetRestaurantsUseCase
import com.fukudai.meshiroulette.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RestaurantListViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantListUiState())
    val uiState: StateFlow<RestaurantListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadRestaurants()
    }

    fun loadRestaurants() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                getRestaurantsUseCase(
                    genres = _uiState.value.selectedGenres.toList().takeIf { it.isNotEmpty() },
                    priceRange = _uiState.value.selectedPriceRange,
                    isOpenNow = _uiState.value.isOpenNowOnly.takeIf { it }
                ).collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true, error = null) }
                        }
                        is NetworkResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    restaurants = result.data,
                                    error = null
                                )
                            }
                        }
                        is NetworkResult.Error -> {
                            _uiState.update {
                                it.copy(isLoading = false, error = result.message)
                            }
                        }
                    }
                }
            } finally {
                if (!isActive) {
                    _uiState.update { it.copy(isLoading = false) }
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
        loadRestaurants()
    }

    fun setPriceRange(priceRange: PriceRange) {
        _uiState.update { it.copy(selectedPriceRange = priceRange) }
        loadRestaurants()
    }

    fun setOpenNowOnly(isOpenNow: Boolean) {
        _uiState.update { it.copy(isOpenNowOnly = isOpenNow) }
        loadRestaurants()
    }

    fun showFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = true) }
    }

    fun hideFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
