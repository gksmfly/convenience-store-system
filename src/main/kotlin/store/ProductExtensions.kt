package store

import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

// 유통기한까지 남은 일수를 계산 (없으면 null 반환)
fun Product.remainingDays(): Long? =
    expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }

// 할인 정책을 적용한 가격 계산
// policy: (남은 일수 → 할인율) 예: 2일 남음 30%, 1일 남음 50%
fun Product.discountedPrice(policy: Map<Int, Double>): Int {
    val days = remainingDays() ?: return price
    val rate = policy[days.toInt()]
        ?: policy.filter { days <= it.key }.minByOrNull { it.key }?.value
        ?: 0.0
    return (price * (1 - rate)).toInt()
}

// Int값을 원화(₩) 형식으로 변환하는 확장 함수
fun Int.krw(): String =
    NumberFormat.getCurrencyInstance(Locale.KOREA).format(this)

// 판매량 기준 상위 N개 상품을 반환
fun Map<String, Int>.topN(n: Int): List<Pair<String, Int>> =
    entries.sortedByDescending { it.value }.take(n).map { it.key to it.value }

// 총 매출(정가 기준) 계산
fun totalRevenue(products: List<Product>, sales: Map<String, Int>): Int =
    sales.entries.sumOf { (name, qty) ->
        (products.find { it.name == name }?.price ?: 0) * qty
    }

// 재고 회전율 = 판매량 / 현재 재고
fun rotationRate(salesQty: Int, currentStock: Int): Double =
    if (currentStock <= 0) 0.0 else salesQty.toDouble() / currentStock