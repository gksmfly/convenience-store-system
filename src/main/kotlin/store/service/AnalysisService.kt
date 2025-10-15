@file:Suppress("PackageDirectoryMismatch")
package store

import java.time.LocalDate

/**
 * 매출/회전/ABC/죽은재고 등 분석 로직
 * - 출력은 하지 않고, 데이터만 반환
 */
class AnalysisService(
    private val clock: Clock,
    private val products: ProductRepository,
    private val sales: SalesRepository
) {
    /** 최근 n일 판매합(상품별) */
    fun salesByProduct(lastNDays: Long = 7): Map<String, Int> {
        val to = clock.today()
        val from = to.minusDays(lastNDays - 1)
        return sales.byDateRange(from, to)
            .groupBy { it.productId }
            .mapValues { (_, v) -> v.sumOf { it.qty } }
    }

    /** Top-N 베스트셀러 (id, qty) */
    fun topN(n: Int, lastNDays: Long = 7): List<Pair<String, Int>> =
        salesByProduct(lastNDays).entries
            .sortedByDescending { it.value }
            .take(n)
            .map { it.key to it.value }

    /** 최근 n일 동안 판매 0개인 '죽은 재고' id 목록 */
    fun deadStock(lastNDays: Long = 7): List<String> {
        val soldIds = salesByProduct(lastNDays).keys
        return products.findAll().map { it.id }.filter { it !in soldIds }
    }

    /** 간단 ABC 분석: 누적 기여도 기준 분류(A≈상위80%, B≈다음15%, C≈나머지) */
    fun abc(lastNDays: Long = 30): Map<String, Char> {
        val to = clock.today()
        val from = to.minusDays(lastNDays - 1)
        val totals = sales.byDateRange(from, to)
            .groupBy { it.productId }
            .mapValues { (_, v) -> v.sumOf { it.priceAtSale * it.qty } }
            .toList()
            .sortedByDescending { it.second }

        val grand = totals.sumOf { it.second }.toDouble().coerceAtLeast(1.0)
        var acc = 0.0
        val result = mutableMapOf<String, Char>()
        for ((id, value) in totals) {
            acc += value
            val ratio = acc / grand
            result[id] = when {
                ratio <= 0.80 -> 'A'
                ratio <= 0.95 -> 'B'
                else -> 'C'
            }
        }
        // 판매 전혀 없는 품목은 C로
        products.findAll().forEach { if (it.id !in result) result[it.id] = 'C' }
        return result
    }
}