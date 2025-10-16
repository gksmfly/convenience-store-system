package store

/*
 * 리포트/요약 출력 담당
 * - 긴급 재고 알림(재고율%/부족수)
 * - 유통기한 임박(권장 할인, 원가→할인가)
 * - 수동 할인 등록 현황
 * - 베스트셀러(수량/매출)
 * - 오늘 매출 요약
 * - 경영 분석(회전율/과다재고/발주권장)
 * - 종합 운영 현황
 * (발주 제안 전용 섹션은 대시보드에서 제외)
 */
class ReportService(
    private val inv: InventoryService,
    private val analysis: AnalysisService,
    private val pricing: PricingService
) {
    /* 긴급 재고 알림: 재고율%와 부족수량 표기 */
    fun stockAlerts(products: List<Product>, threshold: Double = 0.30): String {
        val lines = buildList {
            add("▷ 긴급 재고 알림(≤ ${(threshold * 100).toInt()}%)")
            products.forEach { p ->
                val st = inv.stockStatus(p, threshold)
                if (st != StockStatus.SUFFICIENT) {
                    val shortage = p.targetStock - p.currentStock
                    val ratePct = "%.1f".format(p.currentStock * 100.0 / p.targetStock)
                    add("- ${p.name} (${p.currentStock}/${p.targetStock} → ${ratePct}%) → ${st} / 부족 ${shortage}개")
                }
            }
        }
        return lines.joinToString(System.lineSeparator())
    }

    /* 유통기한 임박: 권장 할인률 + (원가 → 할인가) */
    fun expiringSoonReport(products: List<Product>, warnDays: Int = 3): String {
        val list = inv.expiringSoon(products, warnDays)
        if (list.isEmpty()) return "⏳ 유통기한 임박 목록(D≤$warnDays)\n- 없음"
        val rows = list.joinToString(System.lineSeparator()) { p ->
            val left = inv.daysLeft(p) ?: 0
            val rate = pricing.discountRateByDaysLeft(left)
            val toPrice = discounted(p.price, rate)
            val dateStr = p.expiryDate?.toString() ?: "미관리"
            "- ${p.name}: ${dateStr} (D-$left) → 할인 ${(rate * 100).toInt()}% (${p.price.krw()} → ${toPrice.krw()})"
        }
        return "▷ 유통기한 임박 목록(D≤$warnDays)\n$rows"
    }

    /* 수동 할인 등록 상품 */
    fun manualDiscountsReport(products: List<Product>): String {
        val rows = products.mapNotNull { p ->
            val r = pricing.manualDiscountRate(p.id) ?: return@mapNotNull null
            val toPrice = discounted(p.price, r)
            "- ${p.name}: 수동 할인 ${(r * 100).toInt()}% (${p.price.krw()} → ${toPrice.krw()})"
        }
        return if (rows.isEmpty()) "▷ 할인 등록 상품\n- 없음"
        else "▷ 할인 등록 상품\n" + rows.joinToString(System.lineSeparator())
    }

    /** 최근 N일 베스트셀러 TOP N(수량/매출) */
    fun bestSellersReport(n: Int = 5, lastNDays: Long = 7): String {
        val all = inv.allProducts().associateBy { it.id }
        val top = analysis.topNByRevenue(n, lastNDays)
        if (top.isEmpty()) return "▷ 최근 ${lastNDays}일 베스트셀러 TOP ${n}\n- 데이터 없음"
        val lines = top.mapIndexed { idx, (id, qty, revenue) ->
            val name = all[id]?.name ?: id
            "${idx + 1}위: $name (${qty}개, 매출 ${revenue.krw()})"
        }
        return "▷ 최근 ${lastNDays}일 베스트셀러 TOP ${n}\n" + lines.joinToString(System.lineSeparator())
    }

    /** 오늘 매출 요약(총합 + 품목별 상위) */
    fun todaySalesSummary(topN: Int = 5): String {
        val (qty, revenue) = analysis.todaySales()
        val all = inv.allProducts().associateBy { it.id }
        val top = analysis.topNByRevenue(topN, lastNDays = 1)
        val lines = top.mapIndexed { idx, (id, q, r) ->
            val name = all[id]?.name ?: id
            "${idx + 1}위: $name (${q}개, 매출 ${r.krw()})"
        }
        return listOf(
            "▷ 오늘 총 매출: ${revenue.krw()} (총 ${qty}개 판매)",
            if (lines.isEmpty()) "  - 품목별 매출 데이터 없음"
            else "  - 품목별 상위 ${topN}\n" + lines.joinToString(System.lineSeparator()) { "    $it" }
        ).joinToString(System.lineSeparator())
    }

    /** 경영 분석(회전율 최고/최저, 과다재고, 발주권장) */
    fun managementInsights(lastNDays: Long = 7, threshold: Double = 0.30): String {
        val all = inv.allProducts().associateBy { it.id }
        val rotations = analysis.rotationRates(lastNDays) // (id, sold, rate%)
        val highest = rotations.firstOrNull()
        val lowest  = rotations.lastOrNull()

        val excess = analysis.excessInventory() // (id, 초과수량)
        val lowList = inv.allProducts().filter { inv.needsReorder(it, threshold) }
        val reorderTotalQty = lowList.sumOf { (it.targetStock - it.currentStock).coerceAtLeast(1) }

        val lines = buildList {
            add("▷ 경영 분석 리포트 (입력 데이터 기반 분석)")
            highest?.let { (id, sold, rate) ->
                val p = all[id]!!
                add("- 재고 회전율 최고: ${p.name} (재고 ${p.currentStock}개, ${lastNDays}일 판매 ${sold}개 → ${"%.1f".format(rate)}% 회전)")
                add("- 판매 효율 1위 : ${p.name} (재고 ${p.currentStock} 중 ${sold}개 판매 → ${"%.1f".format(rate)}% 효율)")
            }
            lowest?.let { (id, sold, rate) ->
                val p = all[id]!!
                add("- 재고 회전율 최저: ${p.name} (재고 ${p.currentStock}개, ${lastNDays}일 판매 ${sold}개 → ${"%.1f".format(rate)}% 회전)")
            }
            if (excess.isNotEmpty()) {
                val top2 = excess.take(2).joinToString(", ") { (id, over) -> "${all[id]?.name ?: id} (${over}개)" }
                add("- 재고 과다 품목: $top2")
            } else add("- 재고 과다 품목: 없음")
            add("- 발주 권장: 총 ${lowList.size}개 품목, ${reorderTotalQty}개 수량")
        }
        return lines.joinToString(System.lineSeparator())
    }

    /* 종합 운영 현황(시스템 처리 결과 요약) */
    fun operationsSummary(warnDays: Int = 3, threshold: Double = 0.30): String {
        val list = inv.allProducts()
        val totalItems = list.size
        val totalStock = list.sumOf { it.currentStock }
        val inventoryValue = list.sumOf { it.price * it.currentStock }
        val low = list.filter { inv.needsReorder(it, threshold) }
        val exp = inv.expiringSoon(list, warnDays)
        val (todayQty, todayRevenue) = analysis.todaySales()

        val lines = listOf(
            "▷ 종합 운영 현황 (시스템 처리 결과)",
            "- 전체 등록 상품: ${totalItems}종",
            "- 현재 총 재고: ${totalStock}개",
            "- 현재 재고가치: ${inventoryValue.krw()}",
            "- 재고 부족 상품: ${low.size}종 (30% 이하)",
            "- 유통기한 임박: ${exp.size}종 (3일 이내)",
            "- 오늘 총 판매: ${todayQty}개, 매출 ${todayRevenue.krw()}",
            "- 시스템 처리 완료: 100%"
        )
        return lines.joinToString(System.lineSeparator())
    }

    /** 대시보드(발주 제안 제외, 할인 등록 섹션 포함) */
    fun dashboard(products: List<Product>): String = listOf(
        "=== 24시간 학교 편의점 스마트 재고 관리 시스템 ===",
        stockAlerts(products),
        expiringSoonReport(products),
        manualDiscountsReport(products),
        bestSellersReport(),
        todaySalesSummary(),
        managementInsights(),
        operationsSummary()
    ).joinToString(System.lineSeparator())
}