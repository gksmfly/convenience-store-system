@file:Suppress("PackageDirectoryMismatch")
package store

import java.time.LocalDate

/**
 * 판매 이력(분석/리포트의 시간축 근거)
 * - timestamp를 LocalDate로 단순화(일 단위)
 */
data class Sale(
    val productId: String,
    val qty: Int,
    val priceAtSale: Int,
    val timestamp: LocalDate = LocalDate.now()
)