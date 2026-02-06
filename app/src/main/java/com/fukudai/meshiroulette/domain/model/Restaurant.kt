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
    val googleMapsUrl: String?,
    val openingHours: String?,
    val closingHours: String?
)

enum class Genre(val displayName: String, val apiValue: String) {
    ALL("すべて", ""),
    TEISHOKU("定食", "定食"),
    CHINESE("中華", "中華"),
    RAMEN("ラーメン", "ラーメン"),
    CURRY("カレー", "カレー"),
    UDON("うどん", "うどん"),
    GYUDON("牛丼", "牛丼"),
    TONKATSU("とんかつ", "とんかつ"),
    YAKINIKU("焼肉", "焼肉"),
    IZAKAYA("居酒屋", "居酒屋"),
    CAFE("カフェ", "カフェ"),
    OTHER("その他", "other");

    companion object {
        fun fromApiValue(value: String?): Genre {
            if (value.isNullOrEmpty()) return OTHER
            return entries.find { it.apiValue == value } ?: OTHER
        }
    }
}

enum class PriceRange(val displayName: String, val apiValue: Int) {
    ALL("すべて", 0),
    CHEAP("〜500円", 1),
    MEDIUM("500〜1000円", 2),
    EXPENSIVE("1000円〜", 3);

    companion object {
        fun fromApiValue(value: Int?): PriceRange {
            if (value == null) return MEDIUM
            return entries.find { it.apiValue == value } ?: MEDIUM
        }
    }
}
