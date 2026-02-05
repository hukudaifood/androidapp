package com.fukudai.meshiroulette.domain.usecase

import com.fukudai.meshiroulette.domain.model.Restaurant
import com.fukudai.meshiroulette.domain.repository.RestaurantRepository
import com.fukudai.meshiroulette.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRestaurantDetailUseCase @Inject constructor(
    private val repository: RestaurantRepository
) {
    operator fun invoke(id: String): Flow<NetworkResult<Restaurant>> {
        return repository.getRestaurantDetail(id)
    }
}
