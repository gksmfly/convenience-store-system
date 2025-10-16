package store

import java.time.LocalDate

/** 판매 이력 저장소 인터페이스 */
interface SalesRepository {
    fun record(sale: Sale)
    fun all(): List<Sale>
    fun byDateRange(from: LocalDate, toInclusive: LocalDate): List<Sale>
}

/** 메모리 구현체 */
class InMemorySalesRepository : SalesRepository {
    private val data = mutableListOf<Sale>()

    override fun record(sale: Sale) {
        data += sale
    }

    override fun all(): List<Sale> = data.toList()

    override fun byDateRange(from: LocalDate, toInclusive: LocalDate): List<Sale> =
        data.filter { it.timestamp >= from && it.timestamp <= toInclusive }
}