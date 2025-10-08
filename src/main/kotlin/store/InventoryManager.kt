package store

import java.time.LocalDate

// Phase 3: 시스템 매니저
class InventoryManager(
    private val products: List<Product>,                // 전체 상품
    private val sales: Map<String, Int>,                // 오늘 판매량
    private val stockThreshold: Double = 0.3,           // 재고부족 기준
    private val expiryWarningDays: Long = 3,            // 임박 기준(일)
    private val discountPolicy: Map<Int, Double> =      // 할인정책
        mapOf(3 to 0.0, 2 to 0.3, 1 to 0.5, 0 to 0.7)
) {

    // 🚨 재고 부족 경보
    fun printStockAlerts() {
        println("🚨 긴급 재고 알림 (재고율 ${(stockThreshold * 100).toInt()}% 이하)")
        products.filter { it.stockStatus(stockThreshold) != StockStatus.SUFFICIENT }
            .forEach { p ->
                val rate = "%.1f".format(p.stockRate() * 100)
                println("- ${p.name}(${p.category}): 현재 ${p.currentStock}개 → 적정 ${p.targetStock}개 " +
                        "(${p.shortage()}개 발주 필요) [재고율: $rate%]")
            }
    }

    // ⚠ 유통기한 임박
    fun printExpiryWarnings() {
        println("\n⚠ 유통기한 임박 상품 (${expiryWarningDays}일 이내)")
        products.mapNotNull { p ->
            val d = p.remainingDays() ?: return@mapNotNull null
            if (d <= expiryWarningDays) p to d else null
        }.sortedBy { it.second }.forEach { (p, d) ->
            val label = if (d <= 0) "당일" else "${d}일 남음"
            val after = p.discountedPrice(discountPolicy)
            println("- ${p.name}: $label → ${p.price.krw()} → ${after.krw()}")
        }
    }

    // 📈 베스트셀러 TOP 5  ← 여기 수정됨 (qty 변수 사용)
    fun printBestSellersTop5() {
        println("\n📈 오늘의 베스트셀러 TOP 5")
        sales.topN(5).forEachIndexed { i, (name, qty) ->
            val price = products.find { it.name == name }?.price ?: 0
            println("${i + 1}위: $name (${qty}개 판매, ${(price * qty).krw()})")
        }
    }

    // 💰 매출 요약
    fun printRevenueSummary() {
        println("\n💰 매출 현황")
        val sum = totalRevenue(products, sales)
        println("- 오늘 총 매출: ${sum.krw()} (총 판매 ${sales.values.sum()}개)")
        sales.entries.sortedByDescending { it.value }.forEach { (name, qty) ->
            val price = products.find { it.name == name }?.price ?: 0
            println("  * $name: ${(price * qty).krw()} (${qty}개 × ${price.krw()})")
        }
    }

    // 📋 종합 현황
    fun printOpsSummary() {
        val totalRegistered = products.size
        val totalStock = products.sumOf { it.currentStock }
        val totalStockValue = products.sumOf { it.currentStock * it.price }
        val lowCnt = products.count { it.stockStatus(stockThreshold) != StockStatus.SUFFICIENT }
        val expCnt = products.count { (it.remainingDays() ?: Long.MAX_VALUE) <= expiryWarningDays }

        println("\n📋 종합 운영 현황")
        println("- 전체 등록 상품: ${totalRegistered}종")
        println("- 현재 총 재고: ${totalStock}개")
        println("- 현재 재고가치: ${totalStockValue.krw()}")
        println("- 재고 부족 상품: ${lowCnt}종")
        println("- 유통기한 임박: ${expCnt}종")
    }

    // 전체 리포트 한번에
    fun runDailyReport() {
        println("=== 24시간 학교 편의점 스마트 재고 관리 시스템 ===\n")
        printStockAlerts()
        printExpiryWarnings()
        printBestSellersTop5()
        printRevenueSummary()
        printOpsSummary()
        println("\n시스템 처리 완료: 100%")
    }

    companion object {
        // 과제 샘플 데이터
        fun sample(): InventoryManager {
            val products = listOf(
                Product("새우깡", 1500, ProductCategory.SNACK, 30, 5, null),
                Product("콜라 500ml", 1500, ProductCategory.BEVERAGE, 25, 8, null),
                Product("김치찌개 도시락", 5500, ProductCategory.FOOD, 20, 3, LocalDate.now().plusDays(2)),
                Product("참치마요 삼각김밥", 1500, ProductCategory.FOOD, 15, 12, LocalDate.now().plusDays(1)),
                Product("딸기 샌드위치", 2800, ProductCategory.FOOD, 10, 2, LocalDate.now()),
                Product("물 500ml", 1000, ProductCategory.BEVERAGE, 50, 25, null),
                Product("초코파이", 3000, ProductCategory.SNACK, 20, 15, LocalDate.now().plusDays(1)),
                Product("즉석라면", 1200, ProductCategory.FOOD, 40, 45, LocalDate.now().plusDays(30))
            )
            val todaySales = mapOf(
                "새우깡" to 15,
                "콜라 500ml" to 12,
                "참치마요 삼각김밥" to 10,
                "초코파이" to 8,
                "물 500ml" to 7,
                "딸기 샌드위치" to 3,
                "김치찌개 도시락" to 2
            )
            return InventoryManager(
                products = products,
                sales = todaySales,
                stockThreshold = 0.3,
                expiryWarningDays = 3,
                discountPolicy = mapOf(3 to 0.0, 2 to 0.3, 1 to 0.5, 0 to 0.7)
            )
        }
    }
}