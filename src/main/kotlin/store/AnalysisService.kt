package store

class AnalysisService(
    private val clock: Clock,
    private val products: ProductRepository,
    private val sales: SalesRepository
) {
    /** 최근 N일 상품별 판매 수량 */
    fun salesByProduct(lastNDays: Long = 7): Map<String, Int> {
        val to = clock.today()
        val from = to.minusDays(lastNDays - 1)
        return sales.byDateRange(from, to)
            .groupBy { it.productId }
            .mapValues { (_, v) -> v.sumOf { it.qty } }
    }

    /** 최근 N일 상품별 (수량, 매출) */
    fun salesByProductWithRevenue(lastNDays: Long = 7): Map<String, Pair<Int, Int>> {
        val to = clock.today()
        val from = to.minusDays(lastNDays - 1)
        return sales.byDateRange(from, to)
            .groupBy { it.productId }
            .mapValues { (_, list) ->
                val qty = list.sumOf { it.qty }
                val revenue = list.sumOf { it.qty * it.priceAtSale }
                qty to revenue
            }
    }

    /** 최근 N일 상품별 매출만 */
    fun revenueByProduct(lastNDays: Long = 7): Map<String, Int> =
        salesByProductWithRevenue(lastNDays).mapValues { it.value.second }

    /** 오늘(당일) 총 판매 (수량, 매출) */
    fun todaySales(): Pair<Int, Int> {
        val d = clock.today()
        val list = sales.byDateRange(d, d)
        val qty = list.sumOf { it.qty }
        val revenue = list.sumOf { it.qty * it.priceAtSale }
        return qty to revenue
    }

    /** 최근 N일 판매 수량 기준 TOP N */
    fun topN(n: Int, lastNDays: Long = 7): List<Pair<String, Int>> =
        salesByProduct(lastNDays).entries
            .sortedByDescending { it.value }
            .take(n)
            .map { it.key to it.value }

    /** 최근 N일 매출 기준 TOP N (productId, 수량, 매출) */
    fun topNByRevenue(n: Int, lastNDays: Long = 7): List<Triple<String, Int, Int>> {
        val map = salesByProductWithRevenue(lastNDays)
        return map.entries
            .sortedByDescending { it.value.second }
            .take(n)
            .map { Triple(it.key, it.value.first, it.value.second) }
    }

    /** 재고 회전율 목록: (productId, 최근 N일 판매수량, 회전율%)  회전율 = 판매수량 / 현재고 * 100 */
    fun rotationRates(lastNDays: Long = 7): List<Triple<String, Int, Double>> {
        val sold = salesByProduct(lastNDays)
        val all = products.findAll()
        return all.map { p ->
            val s = sold[p.id] ?: 0
            val denom = if (p.currentStock <= 0) 1 else p.currentStock
            Triple(p.id, s, (s.toDouble() / denom) * 100.0)
        }.sortedByDescending { it.third }
    }

    /** 과다 재고(현재고 > 적정재고) 목록 (productId, 초과수량) */
    fun excessInventory(): List<Pair<String, Int>> =
        products.findAll()
            .filter { it.currentStock > it.targetStock }
            .map { it.id to (it.currentStock - it.targetStock) }
            .sortedByDescending { it.second }

    /** 최근 N일간 판매 내역이 전혀 없는 상품(죽은 재고) */
    fun deadStock(lastNDays: Long = 7): List<String> {
        val sold = salesByProduct(lastNDays).keys
        return products.findAll().map { it.id }.filter { it !in sold }
    }
}