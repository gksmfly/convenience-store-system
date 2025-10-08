package store

import java.time.LocalDate

// 상품의 종류를 구분하는 열거형(enum)
enum class ProductCategory { BEVERAGE, SNACK, FOOD, GOODS }

// 재고 상태를 표현하는 열거형(enum)
enum class StockStatus { SUFFICIENT, LOW, OUT_OF_STOCK }

// 상품 데이터 클래스 (이름, 가격, 카테고리, 재고 등 포함)
data class Product(
    val name: String,                 // 상품명
    val price: Int,                   // 상품 가격
    val category: ProductCategory,    // 카테고리
    val targetStock: Int,             // 적정 재고 수량
    var currentStock: Int,            // 현재 재고 수량
    val expiryDate: LocalDate? = null // 유통기한 (없을 수도 있음)
) {
    // 재고율 계산 (현재 재고 / 적정 재고)
    fun stockRate(): Double =
        if (targetStock == 0) 0.0 else currentStock.toDouble() / targetStock

    // 재고 상태 판단 (기본 기준 30% 이하 LOW)
    fun stockStatus(threshold: Double = 0.3): StockStatus = when {
        currentStock <= 0 -> StockStatus.OUT_OF_STOCK
        stockRate() < threshold -> StockStatus.LOW
        else -> StockStatus.SUFFICIENT
    }

    // 발주 필요 수량 계산 (적정 - 현재)
    fun shortage(): Int = (targetStock - currentStock).coerceAtLeast(0)
}