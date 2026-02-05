package com.fukudai.meshiroulette.data.model

import com.google.gson.annotations.SerializedName

data class RestaurantDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("genre")
    val genre: String,
    @SerializedName("priceRange")
    val priceRange: String,
    @SerializedName("address")
    val address: String,
    @SerializedName("latitude")
    val latitude: Double?,
    @SerializedName("longitude")
    val longitude: Double?,
    @SerializedName("imageUrl")
    val imageUrl: String?,
    @SerializedName("rating")
    val rating: Double?,
    @SerializedName("openingHours")
    val openingHours: String?,
    @SerializedName("description")
    val description: String?
)

data class RestaurantListResponse(
    @SerializedName("restaurants")
    val restaurants: List<RestaurantDto>,
    @SerializedName("total")
    val total: Int
)

data class RouletteRequest(
    @SerializedName("genre")
    val genre: String? = null,
    @SerializedName("priceRange")
    val priceRange: String? = null
)

data class RouletteResponse(
    @SerializedName("restaurant")
    val restaurant: RestaurantDto
)
