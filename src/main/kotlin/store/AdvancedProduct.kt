package store

import java.time.LocalDate

/** 확장성을 위한 공통 인터페이스(선택) */
interface Expirable {
    val expiryDate: LocalDate?
}

open class BaseProduct(
    val name: String,
    val price: Int,
    val targetStock: Int,
    var currentStock: Int
) {
    fun stockRate(): Double = if (targetStock == 0) 0.0 else currentStock.toDouble() / targetStock
    fun shortage(): Int = (targetStock - currentStock).coerceAtLeast(0)
}

class FoodProduct(
    name: String,
    price: Int,
    targetStock: Int,
    currentStock: Int,
    override val expiryDate: LocalDate
) : BaseProduct(name, price, targetStock, currentStock), Expirable

class BeverageProduct(
    name: String,
    price: Int,
    targetStock: Int,
    currentStock: Int
) : BaseProduct(name, price, targetStock, currentStock)