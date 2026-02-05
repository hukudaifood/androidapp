package com.fukudai.meshiroulette.data.repository

import com.fukudai.meshiroulette.data.model.RouletteRequest
import com.fukudai.meshiroulette.data.model.toDomain
import com.fukudai.meshiroulette.data.remote.ApiService
import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.domain.model.PriceRange
import com.fukudai.meshiroulette.domain.model.Restaurant
import com.fukudai.meshiroulette.domain.repository.RestaurantRepository
import com.fukudai.meshiroulette.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class RestaurantRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : RestaurantRepository {

    override fun getRestaurants(
        genre: Genre?,
        priceRange: PriceRange?
    ): Flow<NetworkResult<List<Restaurant>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getRestaurants(
                genre = genre?.takeIf { it != Genre.ALL }?.apiValue,
                priceRange = priceRange?.takeIf { it != PriceRange.ALL }?.apiValue
            )
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(NetworkResult.Success(body.restaurants.toDomain()))
                } ?: emit(NetworkResult.Error("レスポンスが空です"))
            } else {
                emit(NetworkResult.Error("エラーが発生しました", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "不明なエラーが発生しました"))
        }
    }

    override fun getRestaurantDetail(id: String): Flow<NetworkResult<Restaurant>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getRestaurantDetail(id)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(NetworkResult.Success(body.toDomain()))
                } ?: emit(NetworkResult.Error("レスポンスが空です"))
            } else {
                emit(NetworkResult.Error("エラーが発生しました", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "不明なエラーが発生しました"))
        }
    }

    override fun spinRoulette(
        genre: Genre?,
        priceRange: PriceRange?
    ): Flow<NetworkResult<Restaurant>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = RouletteRequest(
                genre = genre?.takeIf { it != Genre.ALL }?.apiValue,
                priceRange = priceRange?.takeIf { it != PriceRange.ALL }?.apiValue
            )
            val response = apiService.spinRoulette(request)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    emit(NetworkResult.Success(body.restaurant.toDomain()))
                } ?: emit(NetworkResult.Error("レスポンスが空です"))
            } else {
                emit(NetworkResult.Error("エラーが発生しました", response.code()))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "不明なエラーが発生しました"))
        }
    }
}
