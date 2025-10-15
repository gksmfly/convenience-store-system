@file:Suppress("PackageDirectoryMismatch")
package store

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Phase 3: 키보드 입력 기반 메뉴형 콘솔 UI
 * - 상품 CRUD(추가/수정/삭제)
 * - 입고/판매 처리
 * - 리포트(재고 경보 / 유통기한 임박 / 베스트셀러 / 발주 제안)
 */
class ConsoleApp(
    private val products: ProductRepository,
    private val inv: InventoryService,
    private val analysis: AnalysisService,
    private val report: ReportService
) {
    private val df = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun run() {
        while (true) {
            println("\n=== 24시간 편의점 재고 관리 ===")
            println("1. 전체 리포트 보기")
            println("2. 상품 판매 등록")
            println("3. 상품 입고 등록")
            println("4. 상품 추가")
            println("5. 상품 수정")
            println("6. 상품 삭제")
            println("7. 상품 목록/검색")
            println("8. 발주 제안 보기 (ROP)")
            println("0. 종료")
            print("선택> ")

            when (readLine()?.trim()) {
                "1" -> showReports()
                "2" -> sellProduct()
                "3" -> receiveProduct()
                "4" -> addProduct()
                "5" -> updateProduct()
                "6" -> deleteProduct()
                "7" -> listOrSearch()
                "8" -> showReorderSuggestion()
                "0" -> { println("종료합니다."); return }
                else -> println("잘못된 입력입니다.")
            }
        }
    }

    // ----------------- 리포트 -----------------
    private fun showReports() {
        val list = products.findAll()
        println("\n[리포트]")
        println(report.stockAlerts(list, threshold = 0.30))
        println()
        println(report.expiryAlerts(list, top = 5))
        println()
        println(report.bestSellers(products, n = 5))
    }

    private fun showReorderSuggestion() {
        val msg = report.reorderSuggestions(
            productsRepo = products,
            avgDaily = { p: Product ->
                // NOTE: salesByProduct()의 인자는 Long → 7L
                val sold7 = analysis.salesByProduct(7L)[p.id] ?: 0
                sold7 / 7.0
            }
            // leadDays, safety는 기본값(2, 5) 사용
        )
        println(msg)
    }

    // ----------------- 판매/입고 -----------------
    private fun sellProduct() {
        val id = prompt("상품 ID")
        val qty = promptInt("판매 수량(1 이상)") ?: return
        try {
            inv.sell(id, qty)
            println("판매 등록 완료: id=$id, qty=$qty")
        } catch (e: IllegalArgumentException) {
            println("오류: ${e.message}")
        }
    }

    private fun receiveProduct() {
        val id = prompt("상품 ID")
        val qty = promptInt("입고 수량(1 이상)") ?: return
        try {
            inv.receiveStock(id, qty)
            println("입고 등록 완료: id=$id, qty=$qty")
        } catch (e: IllegalArgumentException) {
            println("오류: ${e.message}")
        }
    }

    // ----------------- 상품 CRUD -----------------
    private fun addProduct() {
        val id = prompt("새 상품 ID (중복 불가)")
        if (products.findById(id) != null) {
            println("이미 존재하는 ID 입니다."); return
        }
        val name = prompt("상품명")
        val category = pickCategory() ?: return
        val price = promptInt("가격(원)") ?: return
        val target = promptInt("적정 재고") ?: return
        val current = promptInt("현재 재고") ?: return
        val expiry = promptDateOrNull("유통기한(yyyy-MM-dd, 미입력시 없음)")
        val barcode = promptOptional("바코드(선택)")

        val p = Product(
            id = id,
            name = name,
            category = category,
            price = price,
            targetStock = target,
            currentStock = current,
            expiryDate = expiry,
            barcode = barcode?.ifBlank { null }
        )
        products.save(p)
        println("상품 추가 완료: $name ($id)")
    }

    private fun updateProduct() {
        val id = prompt("수정할 상품 ID")
        val origin = products.findById(id)
        if (origin == null) {
            println("해당 ID의 상품이 없습니다."); return
        }
        println("기존: $origin")
        val name = promptOptional("상품명(Enter=유지)")?.takeIf { it.isNotBlank() } ?: origin.name
        val catSel = promptOptional("카테고리 번호(Enter=유지) ${ProductCategory.values().withIndexString()}")
        val category = catSel?.toIntOrNull()?.let { ProductCategory.values().getOrNull(it) } ?: origin.category
        val price = promptOptional("가격(원, Enter=유지)")?.toIntOrNull() ?: origin.price
        val target = promptOptional("적정 재고(Enter=유지)")?.toIntOrNull() ?: origin.targetStock
        val current = promptOptional("현재 재고(Enter=유지)")?.toIntOrNull() ?: origin.currentStock
        val expiry = promptOptional("유통기한 yyyy-MM-dd(Enter=유지/빈칸=없음)")?.let {
            if (it.isBlank()) null else LocalDate.parse(it, df)
        } ?: origin.expiryDate
        val barcode = promptOptional("바코드(Enter=유지/빈칸=없음)")?.let { if (it.isBlank()) null else it } ?: origin.barcode

        products.save(origin.copy(
            name = name, category = category, price = price,
            targetStock = target, currentStock = current, expiryDate = expiry, barcode = barcode
        ))
        println("상품 수정 완료: $id")
    }

    private fun deleteProduct() {
        val id = prompt("삭제할 상품 ID")
        val p = products.findById(id)
        if (p == null) {
            println("해당 ID의 상품이 없습니다."); return
        }
        val ok = prompt("정말 삭제하시겠습니까? (y/N)").lowercase() == "y"
        if (ok) {
            products.delete(id)
            println("삭제 완료: $id")
        } else println("취소됨")
    }

    private fun listOrSearch() {
        val key = promptOptional("검색 키워드(Enter=전체)")
        val list = products.findAll()
            .filter { key.isNullOrBlank() || it.name.contains(key!!, ignoreCase = true) || it.id.contains(key!!, true) }
        if (list.isEmpty()) { println("결과 없음"); return }
        println("\n[상품 목록] (${list.size}건)")
        println(String.format("%-6s | %-18s | %-8s | %8s | %6s | %6s | %10s",
            "ID", "이름", "카테고리", "가격", "적정", "현재", "유통기한"))
        list.forEach { p ->
            println(String.format("%-6s | %-18s | %-8s | %8s | %6d | %6d | %10s",
                p.id, p.name, p.category.name, p.price.krw(), p.targetStock, p.currentStock,
                p.expiryDate?.toString() ?: "-"))
        }
    }

    // ----------------- 입력 유틸 -----------------
    private fun prompt(msg: String): String {
        print("$msg: ")
        return readLine()?.trim().orEmpty()
    }

    private fun promptOptional(msg: String): String? {
        print("$msg: ")
        return readLine()?.trim()
    }

    private fun promptInt(msg: String): Int? {
        print("$msg: ")
        val v = readLine()?.trim()?.toIntOrNull()
        if (v == null || v <= 0) {
            println("정수(1 이상)만 입력하세요."); return null
        }
        return v
    }

    private fun promptDateOrNull(msg: String): LocalDate? {
        print("$msg: ")
        val raw = readLine()?.trim().orEmpty()
        if (raw.isBlank()) return null
        return try { LocalDate.parse(raw, df) } catch (_: Exception) {
            println("날짜 형식 오류(예: 2025-10-11)"); null
        }
    }

    private fun pickCategory(): ProductCategory? {
        println("카테고리 선택: ${ProductCategory.values().withIndexString()}")
        val idx = readLine()?.trim()?.toIntOrNull()
        val cat = idx?.let { ProductCategory.values().getOrNull(it) }
        if (cat == null) println("잘못된 선택입니다.")
        return cat
    }

    private fun <E> Array<E>.withIndexString(): String =
        this.mapIndexed { i, v -> "$i:$v" }.joinToString("  ")
}