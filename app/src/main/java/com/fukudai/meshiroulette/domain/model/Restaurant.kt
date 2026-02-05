package com.fukudai.meshiroulette.domain.model

data class Restaurant(
    val id: String,
    val name: String,
    val genre: Genre,
    val priceRange: PriceRange,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val imageUrl: String?,
    val rating: Double?,
    val openingHours: String?,
    val description: String?
)

enum class Genre(val displayName: String, val apiValue: String) {
    ALL("すべて", ""),
    JAPANESE("和食", "japanese"),
    CHINESE("中華", "chinese"),
    WESTERN("洋食", "western"),
    RAMEN("ラーメン", "ramen"),
    CURRY("カレー", "curry"),
    FAST_FOOD("ファストフード", "fast_food"),
    CAFE("カフェ", "cafe"),
    OTHER("その他", "other");

    companion object {
        fun fromApiValue(value: String): Genre {
            return entries.find { it.apiValue == value } ?: OTHER
        }
    }
}

enum class PriceRange(val displayName: String, val apiValue: String) {
    ALL("すべて", ""),
    CHEAP("〜500円", "cheap"),
    MEDIUM("500〜1000円", "medium"),
    EXPENSIVE("1000円〜", "expensive");

    companion object {
        fun fromApiValue(value: String): PriceRange {
            return entries.find { it.apiValue == value } ?: MEDIUM
        }
    }
}
