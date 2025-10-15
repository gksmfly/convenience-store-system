@file:Suppress("PackageDirectoryMismatch")
package store

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

/** 원화 포맷팅: 1800.krw() → ₩1,800 */
fun Int.krw(): String = NumberFormat.getCurrencyInstance(Locale.KOREA).format(this)

/**
 * 단순 할인 적용.
 * - rate: 0.3 → 30% 할인
 * - 정책: 소수점 내림 + 음수 방지
 */
fun discounted(price: Int, rate: Double): Int =
    max(0.0, price * (1.0 - rate)).toInt()