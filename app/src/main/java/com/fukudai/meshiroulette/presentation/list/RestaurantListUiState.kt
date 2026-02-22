package com.fukudai.meshiroulette.presentation.list

import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.domain.model.PriceRange
import com.fukudai.meshiroulette.domain.model.Restaurant

data class RestaurantListUiState(
    val isLoading: Boolean = false,
    val restaurants: List<Restaurant> = emptyList(),
    val selectedGenres: Set<Genre> = emptySet(),
    val selectedPriceRange: PriceRange = PriceRange.ALL,
    val isOpenNowOnly: Boolean = false,
    val showFilterSheet: Boolean = false,
    val error: String? = null
)
