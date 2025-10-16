package store
import java.time.LocalDate

class Product(
    val id: String,
    var name: String,
    val category: ProductCategory,
    var price: Int,
    initialReorderLevel: Int,
    initialStock: Int,
    val expiry: LocalDate? = null
) {
    var reorderLevel: Int = initialReorderLevel
        private set

    var stock: Int = initialStock
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

    // 엔티티 동등성(선택) — ID 기준
    override fun equals(other: Any?) = other is Product && other.id == id
    override fun hashCode(): Int = id.hashCode()
}