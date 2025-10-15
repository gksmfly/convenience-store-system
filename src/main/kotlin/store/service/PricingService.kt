@file:Suppress("PackageDirectoryMismatch")
package store

/**
 * 가격/할인 정책 담당.
 * - daysLeft에 따른 구간 할인률 예시를 제공
 */
class PricingService {
    /** D-Day 기준 단순 정책(예시): D-1 40%, D-2 30%, D-3 20%, 그 외 0% */
    fun discountRateByDaysLeft(daysLeft: Int?): Double = when {
        daysLeft == null        -> 0.0
        daysLeft <= 0           -> 0.5  // 당일 또는 지남: 50% 처리(폐기 직전)
        daysLeft == 1           -> 0.4
        daysLeft == 2           -> 0.3
        daysLeft == 3           -> 0.2
        else                    -> 0.0
    }

    /** 정책 기반 최종 판매가 계산 */
    fun finalPrice(price: Int, daysLeft: Int?): Int =
        discounted(price, discountRateByDaysLeft(daysLeft))
}