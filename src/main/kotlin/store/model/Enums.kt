@file:Suppress("PackageDirectoryMismatch")
package store

/** 상품 분류(과제 범위에서 간단히) */
enum class ProductCategory { BEVERAGE, SNACK, FOOD, DAILY }

/** 재고 상태(대시보드 경보에 사용) */
enum class StockStatus { SUFFICIENT, LOW, OUT_OF_STOCK }