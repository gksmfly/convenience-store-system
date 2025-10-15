@file:Suppress("PackageDirectoryMismatch")
package store

import java.time.Period

/**
 * 재고/입출고/만료/발주점 등 기본 로직
 * - 저장소를 주입받아 실제 상태를 갱신
 */
class InventoryService(
    private val clock: Clock,
    private val products: ProductRepository,
    private val sales: SalesRepository,
    private val pricing: PricingService
) {
    /** 재고 상태 판정(기본 임계치 30%) */
    fun stockStatus(p: Product, threshold: Double = 0.3): StockStatus = when {
        p.currentStock <= 0 -> StockStatus.OUT_OF_STOCK
        p.currentStock.toDouble() / p.targetStock < threshold -> StockStatus.LOW
        else -> StockStatus.SUFFICIENT
    }

    /** 유통기한까지 남은 일수(null이면 계산 불가) */
    fun daysLeft(p: Product): Int? = p.expiryDate?.let {
        Period.between(clock.today(), it).days
    }

    /** 입고(수량 +) */
    fun receiveStock(id: String, qty: Int) {
        val p = products.findById(id) ?: return
        products.save(p.copy(currentStock = (p.currentStock + qty).coerceAtLeast(0)))
    }

    /** 판매(수량 -) + 판매 이력 기록 */
    fun sell(id: String, qty: Int) {
        val p = products.findById(id) ?: return
        val newQty = (p.currentStock - qty).coerceAtLeast(0)
        products.save(p.copy(currentStock = newQty))
        sales.record(Sale(productId = id, qty = qty, priceAtSale = pricing.finalPrice(p.price, daysLeft(p))))
    }

    /** 발주점(평균일판매량×리드타임 + 안전재고) */
    fun reorderPoint(avgDaily: Double, leadDays: Int, safety: Int): Int =
        (avgDaily * leadDays).toInt() + safety
}