package com.fukudai.meshiroulette.data.model

import com.google.gson.annotations.SerializedName

data class RestaurantDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("genre")
    val genre: String?,
    @SerializedName("price_range")
    val priceRange: Int?,
    @SerializedName("address")
    val address: String?,
    @SerializedName("latitude")
    val latitude: Double?,
    @SerializedName("longitude")
    val longitude: Double?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("google_maps_url")
    val googleMapsUrl: String?,
    @SerializedName("opening_hours")
    val openingHours: String?,
    @SerializedName("closing_hours")
    val closingHours: String?
)

data class RouletteRequest(
    @SerializedName("genre")
    val genre: String? = null,
    @SerializedName("price_range")
    val priceRange: Int? = null
)
