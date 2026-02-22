package com.fukudai.meshiroulette.presentation.roulette

import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.domain.model.PriceRange
import com.fukudai.meshiroulette.domain.model.Restaurant

data class RouletteUiState(
    val isSpinning: Boolean = false,
    val selectedRestaurant: Restaurant? = null,
    val selectedGenres: Set<Genre> = emptySet(),
    val selectedPriceRange: PriceRange = PriceRange.ALL,
    val isOpenNowOnly: Boolean = false,
    val showFilterSheet: Boolean = false,
    val error: String? = null,
    val candidateRestaurants: List<Restaurant> = emptyList()
)
