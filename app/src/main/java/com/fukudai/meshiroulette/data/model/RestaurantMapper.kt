package com.fukudai.meshiroulette.data.model

import com.fukudai.meshiroulette.domain.model.Genre
import com.fukudai.meshiroulette.domain.model.PriceRange
import com.fukudai.meshiroulette.domain.model.Restaurant

fun RestaurantDto.toDomain(): Restaurant {
    return Restaurant(
        id = id,
        name = name,
        genre = Genre.fromApiValue(genre),
        priceRange = PriceRange.fromApiValue(priceRange),
        address = address,
        latitude = latitude,
        longitude = longitude,
        imageUrl = imageUrl,
        rating = rating,
        openingHours = openingHours,
        description = description
    )
}

fun List<RestaurantDto>.toDomain(): List<Restaurant> {
    return map { it.toDomain() }
}
