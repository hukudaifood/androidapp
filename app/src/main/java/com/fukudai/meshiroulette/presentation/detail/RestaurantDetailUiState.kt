package com.fukudai.meshiroulette.presentation.detail

import com.fukudai.meshiroulette.domain.model.Restaurant

data class RestaurantDetailUiState(
    val isLoading: Boolean = false,
    val restaurant: Restaurant? = null,
    val error: String? = null
)
