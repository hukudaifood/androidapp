package com.fukudai.meshiroulette.data.model

import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.domain.model.PriceRange
import com.fukudai.meshiroulette.domain.model.Restaurant

fun RestaurantDto.toDomain(): Restaurant {
    return Restaurant(
        id = id.orEmpty(),
        name = name.orEmpty(),
        genre = Genre.fromApiValue(genre),
        priceRange = PriceRange.fromApiValue(priceRange),
        address = address.orEmpty(),
        latitude = latitude,
        longitude = longitude,
        imageUrl = imageUrl,
        googleMapsUrl = googleMapsUrl,
        openingHours = openingHours,
        closingHours = closingHours
    )
}

fun List<RestaurantDto?>.toDomain(): List<Restaurant> {
    return mapNotNull { it?.toDomain() }
}
