package com.fukudai.meshiroulette.presentation.roulette

import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.domain.model.PriceRange
import com.fukudai.meshiroulette.domain.model.Restaurant

data class RouletteUiState(
    val isSpinning: Boolean = false,
    val selectedRestaurant: Restaurant? = null,
    val selectedGenre: Genre = Genre.ALL,
    val selectedPriceRange: PriceRange = PriceRange.ALL,
    val showFilterSheet: Boolean = false,
    val error: String? = null
)
