package com.fukudai.meshiroulette.domain.repository

import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.domain.model.PriceRange
import com.fukudai.meshiroulette.domain.model.Restaurant
import com.fukudai.meshiroulette.util.NetworkResult
import kotlinx.coroutines.flow.Flow

interface RestaurantRepository {

    fun getRestaurants(
        genres: List<Genre>? = null,
        priceRange: PriceRange? = null,
        isOpenNow: Boolean? = null
    ): Flow<NetworkResult<List<Restaurant>>>

    fun getRestaurantDetail(id: String): Flow<NetworkResult<Restaurant>>

    fun spinRoulette(
        genres: List<Genre>? = null,
        priceRange: PriceRange? = null,
        isOpenNow: Boolean? = null
    ): Flow<NetworkResult<Restaurant>>
}
