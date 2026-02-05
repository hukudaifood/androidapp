package com.fukudai.meshiroulette.data.remote

import com.fukudai.meshiroulette.data.model.RestaurantDto
import com.fukudai.meshiroulette.data.model.RestaurantListResponse
import com.fukudai.meshiroulette.data.model.RouletteRequest
import com.fukudai.meshiroulette.data.model.RouletteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("v1/restaurants")
    suspend fun getRestaurants(
        @Query("genre") genre: String? = null,
        @Query("priceRange") priceRange: String? = null
    ): Response<RestaurantListResponse>

    @GET("v1/restaurants/{id}")
    suspend fun getRestaurantDetail(
        @Path("id") id: String
    ): Response<RestaurantDto>

    @POST("v1/roulette")
    suspend fun spinRoulette(
        @Body request: RouletteRequest
    ): Response<RouletteResponse>
}
