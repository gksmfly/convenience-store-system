package store

/*
 * 가격/할인 정책 서비스
 * - daysLeft 기반 기본 할인 정책
 * - 수동 할인 등록(상품별) 기능 추가: 등록이 있으면 기본 정책보다 우선 적용
 */


class PricingService(
    private val discountPolicy: Map<Int, Double> = mapOf(
        Int.MAX_VALUE to 0.0, // 넉넉하면 할인 없음
        3 to 0.0,             // D-3 이상: 0%
        2 to 0.3,             // D-2: 30%
        1 to 0.5,             // D-1: 50%
        0 to 0.7,             // D-day: 70%
        Int.MIN_VALUE to 0.7  // 마이너스(지남): 안전상 동일 70%
    )
) {
    // 수동 할인 등록 저장소 (productId -> rate[0.0..1.0])
    private val manual = mutableMapOf<String, Double>()

    /** D-Day 기반 할인률 */
    fun discountRateByDaysLeft(daysLeft: Int?): Double {
        if (daysLeft == null) return 0.0
        val key = discountPolicy.keys.sortedDescending().firstOrNull { daysLeft >= it } ?: 0
        return discountPolicy[key] ?: 0.0
    }

    /** 수동 할인 현재치 조회(없으면 null) */
    fun manualDiscountRate(productId: String): Double? = manual[productId]

    /** 현재 적용 할인률(수동 우선, 없으면 D-Day 정책) */
    fun currentDiscountRate(productId: String, daysLeft: Int?): Double =
        manual[productId] ?: discountRateByDaysLeft(daysLeft)

    /** 수동 할인 등록/갱신 (0..100%) */
    fun setManualDiscount(productId: String, percent: Int) {
        require(percent in 0..100) { "할인 퍼센트는 0~100 사이여야 합니다." }
        manual[productId] = percent / 100.0
    }

    /** 수동 할인 해제 */
    fun clearManualDiscount(productId: String) {
        manual.remove(productId)
    }
}