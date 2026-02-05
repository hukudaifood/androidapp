package com.fukudai.meshiroulette.domain.usecase

import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.domain.model.PriceRange
import com.fukudai.meshiroulette.domain.model.Restaurant
import com.fukudai.meshiroulette.domain.repository.RestaurantRepository
import com.fukudai.meshiroulette.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRestaurantsUseCase @Inject constructor(
    private val repository: RestaurantRepository
) {
    operator fun invoke(
        genre: Genre? = null,
        priceRange: PriceRange? = null
    ): Flow<NetworkResult<List<Restaurant>>> {
        return repository.getRestaurants(genre, priceRange)
    }
}
