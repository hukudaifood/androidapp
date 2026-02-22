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
        genres: List<Genre>?,
        priceRange: PriceRange?,
        isOpenNow: Boolean?
    ): Flow<NetworkResult<List<Restaurant>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = apiService.getRestaurants(
                genres = genres?.map { it.apiValue }?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() },
                priceRange = priceRange?.takeIf { it != PriceRange.ALL }?.apiValue,
                isOpenNow = isOpenNow?.takeIf { it }
            )
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
        genres: List<Genre>?,
        priceRange: PriceRange?,
        isOpenNow: Boolean?
    ): Flow<NetworkResult<Restaurant>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = RouletteRequest(
                genres = genres?.map { it.apiValue }?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() },
                priceRange = priceRange?.takeIf { it != PriceRange.ALL }?.apiValue,
                onlyOpenNow = isOpenNow?.takeIf { it }
            )
            val response = apiService.spinRoulette(request)
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
}
