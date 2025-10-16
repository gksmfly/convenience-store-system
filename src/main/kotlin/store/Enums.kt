package store

enum class ProductCategory(
    val displayName: String,
    val aliases: List<String> = emptyList()
) {
    BEVERAGE("음료", listOf("음료", "drink", "beverage")),
    SNACK("과자/스낵", listOf("과자", "스낵", "snack")),
    CONVENIENCE_MEAL("간편식", listOf("간편식", "도시락", "샌드위치", "김밥", "meal")),
    ICE_CREAM("아이스크림", listOf("아이스크림", "icecream", "ice_cream")),
    DAIRY("유제품", listOf("유제품", "치즈", "dairy", "milk", "cheese")),
    FROZEN("냉장/냉동 가공식품", listOf("냉장", "냉동", "가공식품", "frozen", "chilled")),
    HOUSEHOLD("생활용품", listOf("생활용품", "위생", "주방", "욕실", "household")),
    ETC("기타", listOf("기타", "etc", "other"));

    companion object {
        fun parse(input: String?): ProductCategory? {
            if (input.isNullOrBlank()) return null
            val s = input.trim().lowercase()
            return entries.firstOrNull { cat ->
                cat.name.equals(s, ignoreCase = true) ||
                        cat.displayName == input ||
                        cat.aliases.any { it.equals(s, ignoreCase = true) }
            }
        }
    }
}

enum class StockStatus {
    SUFFICIENT,
    LOW,
    OUT_OF_STOCK
}