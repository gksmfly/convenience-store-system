package store

import java.time.LocalDate

/**
 * 편의점 상품의 스냅샷(현재 상태)
 * - id: 불변 식별자(문자열)
 * - targetStock/currentStock: 적정/현재 재고
 * - expiryDate: 유통기한(없으면 null)
 */
data class Product(
    val id: ProductId,
    var name: String,
    val category: ProductCategory,
    var price: Int,
    reorderLevel: Int,
    stock: Int,
    val expiry: LocalDate? = null,
) {
    var reorderLevel: Int = reorderLevel
        private set

    var stock: Int = stock
        private set

    fun restock(qty: Int) {
        require(qty > 0) { "입고 수량은 0보다 커야 합니다." }
        stock += qty
    }

    fun sell(qty: Int) {
        require(qty > 0) { "판매 수량은 0보다 커야 합니다." }
        require(qty <= stock) { "재고 부족" }
        stock -= qty
    }

    fun changePrice(newPrice: Int) {
        require(newPrice >= 0) { "가격은 음수 불가" }
        price = newPrice
    }
}
@JvmInline value class ProductId(val value: String)