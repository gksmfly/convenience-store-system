@file:Suppress("PackageDirectoryMismatch")
package store

/**
 * 콘솔/파일/웹 등 어떤 출력에도 재사용 가능한 "보고용 문자열"을 생성하는 서비스.
 * - 출력 포맷은 단순 텍스트로 두되, 데이터 계산은 Inventory/Analysis/Pricing에 위임한다.
 */
class ReportService(
    private val inv: InventoryService,
    private val analysis: AnalysisService,
    private val pricing: PricingService
) {

    /**
     * 재고 경보 요약.
     * @param products 보고 대상 상품 목록
     * @param threshold 적정재고 대비 부족 판단 임계치(기본 30%)
     */
    fun stockAlerts(products: List<Product>, threshold: Double = 0.30): String {
        val lines = buildList {
            add("⚠️  긴급 재고 알림(≤ ${(threshold * 100).toInt()}%)")
            products.forEach { p ->
                val st = inv.stockStatus(p, threshold)
                if (st != StockStatus.SUFFICIENT) {
                    val shortage = (p.targetStock - p.currentStock).coerceAtLeast(0)
                    add("- ${p.name} : $st (부족 ${shortage}개)")
                }
            }
        }
        return if (lines.size == 1) "✅ 재고 경보 없음" else lines.joinToString("\n")
    }

    /**
     * 유통기한 임박/할인 요약.
     * 남은 일수 오름차순으로 상위 [top]개만 보여준다.
     */
    fun expiryAlerts(products: List<Product>, top: Int = 5): String {
        val imminents = products
            .map { it to inv.daysLeft(it) }
            .filter { it.second != null }           // 유통기한 있는 상품만
            .sortedBy { it.second }                 // 남은 일수 오름차순
            .take(top)

        if (imminents.isEmpty()) return "✅ 유통기한 임박 상품 없음"

        return buildString {
            appendLine("⏳ 유통기한 임박(상위 $top)")
            imminents.forEach { (p, d) ->
                val price = pricing.finalPrice(p.price, d)
                val dLabel = if ((d ?: 0) >= 0) "D-${d}" else "D+${kotlin.math.abs(d ?: 0)}"
                appendLine("- ${p.name}: $dLabel  → ${price.krw()}")
            }
        }
    }

    /**
     * 베스트셀러 Top-N (최근 7일 기준).
     * repository를 받아 id→name 매핑을 만든다.
     */
    fun bestSellers(productsRepo: ProductRepository, n: Int = 3): String {
        val top = analysis.topN(n)
        if (top.isEmpty()) return "📈 베스트셀러 데이터 없음"

        val nameById = productsRepo.findAll().associateBy({ it.id }, { it.name })
        return buildString {
            appendLine("🏆 베스트셀러 Top $n (최근 7일)")
            top.forEachIndexed { idx, (id, qty) ->
                appendLine("${idx + 1}. ${nameById[id] ?: id}  (${qty}개)")
            }
        }
    }

    /**
     * ROP(발주점) 기반 발주 제안.
     * - avgDaily: 상품별 최근 평균 일판매량을 계산해주는 함수(외부에서 주입).
     * - leadDays/safety: 리드타임/안전재고 파라미터.
     *
     * 예시 호출:
     *   report.reorderSuggestions(productsRepo) { p ->
     *       val sold7 = analysis.salesByProduct(7L)[p.id] ?: 0
     *       sold7 / 7.0
     *   }
     */
    fun reorderSuggestions(
        productsRepo: ProductRepository,
        avgDaily: (Product) -> Double,
        leadDays: Int = 2,
        safety: Int = 5
    ): String {
        val list = productsRepo.findAll()
        val lines = buildList {
            add("📦 발주 제안")
            list.forEach { p ->
                val rop = inv.reorderPoint(avgDaily(p), leadDays, safety)
                if (p.currentStock <= rop) {
                    val qty = (p.targetStock - p.currentStock).coerceAtLeast(0)
                    add("- ${p.name}: ROP=$rop, 현재=${p.currentStock} → 발주 ${qty}개")
                }
            }
        }
        return if (lines.size == 1) "📦 발주 필요 없음" else lines.joinToString("\n")
    }
}