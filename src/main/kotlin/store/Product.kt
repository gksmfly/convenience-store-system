package store

import java.time.LocalDate

/**
 * 편의점 상품의 스냅샷(현재 상태)
 * - id: 불변 식별자(문자열)
 * - targetStock/currentStock: 적정/현재 재고
 * - expiryDate: 유통기한(없으면 null)
 */
data class Product(
    val id: String,
    val name: String,
    val category: ProductCategory,
    val price: Int,                 // KRW, 원 단위
    val targetStock: Int,
    val currentStock: Int,
    val expiryDate: LocalDate? = null,
    val barcode: String? = null
)