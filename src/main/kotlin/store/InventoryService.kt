package store

import java.time.temporal.ChronoUnit

class InventoryService(
    private val clock: Clock,
    private val products: ProductRepository,
    private val sales: SalesRepository,
    private val pricing: PricingService
) {
    fun stockStatus(p: Product, threshold: Double = 0.3): StockStatus = when {
        p.currentStock <= 0 -> StockStatus.OUT_OF_STOCK
        p.currentStock.toDouble() / p.targetStock < threshold -> StockStatus.LOW
        else -> StockStatus.SUFFICIENT
    }

    // D-day 계산 정확화: ChronoUnit.DAYS
    fun daysLeft(p: Product): Int? = p.expiryDate?.let {
        ChronoUnit.DAYS.between(clock.today(), it).toInt()
    }

    fun receive(id: String, qty: Int) {
        val p = products.findById(id) ?: error("상품을 찾을 수 없음: $id")
        require(qty > 0) { "입고 수량은 양수여야 합니다." }
        products.save(p.copy(currentStock = p.currentStock + qty))
    }

    fun sell(id: String, qty: Int) {
        val p = products.findById(id) ?: error("상품을 찾을 수 없음: $id")
        require(qty > 0) { "판매 수량은 양수여야 합니다." }
        require(p.currentStock >= qty) { "재고 부족: 현재 ${p.currentStock}, 요청 $qty" }

        // 🔽 할인 적용: 수동 할인(있으면) > D-Day 기반
        val left = daysLeft(p)
        val rate = pricing.currentDiscountRate(id, left)
        val priceAtSale = discounted(p.price, rate)

        products.save(p.copy(currentStock = p.currentStock - qty))
        sales.record(Sale(productId = id, qty = qty, priceAtSale = priceAtSale))
    }

    // 폐기 처리: 판매 없이 재고만 감소
    fun dispose(id: String, qty: Int) {
        val p = products.findById(id) ?: error("상품을 찾을 수 없음: $id")
        require(qty > 0) { "폐기 수량은 양수여야 합니다." }
        require(p.currentStock >= qty) { "재고 부족: 현재 ${p.currentStock}, 요청 $qty" }
        products.save(p.copy(currentStock = p.currentStock - qty))
    }

    fun needsReorder(p: Product, threshold: Double = 0.3): Boolean =
        p.currentStock.toDouble() / p.targetStock < threshold

    fun expiringSoon(list: List<Product>, warnDays: Int = 3): List<Product> =
        list.filter { p -> daysLeft(p)?.let { it in 0..warnDays } ?: false }

    fun allProducts(): List<Product> = products.findAll()
    fun upsert(product: Product) = products.save(product)
    fun delete(id: String) = products.delete(id)
}