package store

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ConsoleApp(
    private val products: ProductRepository,
    private val inv: InventoryService,
    private val analysis: AnalysisService,
    private val report: ReportService,
    private val pricing: PricingService
) {
    private val dtf: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun run() {
        while (true) {
            println()
            println("=== 24시간 편의점 재고 관리 ===")
            println("1. 전체 리포트 보기")
            println("2. 상품 판매 등록")
            println("3. 상품 입고 등록")
            println("4. 상품 추가")
            println("5. 상품 수정")
            println("6. 상품 삭제")
            println("7. 상품 목록/검색")
            println("0. 종료")
            println("==========================")
            print("선택> ")

            when (readLine()?.trim()) {
                "1" -> showReport()
                "2" -> sellFlow()
                "3" -> receiveFlow()
                "4" -> addProductFlow()
                "5" -> editProductFlow()    // 목록을 먼저 보여줌
                "6" -> deleteProductFlow()  // 목록 + 존재검사 + 확인
                "7" -> listProducts()       // 목록 후 검색 + 서브메뉴(2~6)
                "0" -> return
                else -> println("올바른 값을 입력하세요.")
            }
        }
    }

    // =============== 공용 출력/유틸 ===============

    // 간단 목록(수정/삭제 진입 전에 보여주는 압축형)
    private fun printCompactList() {
        val list = inv.allProducts()
        if (list.isEmpty()) {
            println("등록된 상품이 없습니다.")
            return
        }
        println("[상품 목록 - 간단 보기] (${list.size}건)")
        println("ID\t|\t이름\t|\t재고")
        list.forEach { p -> println("${p.id}\t|\t${p.name}\t|\t${p.currentStock}") }
    }

    // 유통기한 표기 통일
    private fun renderExpiry(p: Product): String {
        if (p.expiryDate == null) return "미관리"
        val left = inv.daysLeft(p) ?: return p.expiryDate.toString()
        return if (left < 0) "만료(${p.expiryDate})" else "${p.expiryDate} (D-$left)"
    }

    // 권장 입고량 계산: 적정-현재, 최소 1
    private fun recommendedReceiveQty(p: Product): Int =
        (p.targetStock - p.currentStock).coerceAtLeast(1)

    // 입고 권장 목록(부족한 상품만, 재고율 낮은 순)
    private fun printReceiveCandidates() {
        val candidates = inv.allProducts()
            .filter { it.currentStock < it.targetStock }
            .sortedBy { it.currentStock.toDouble() / it.targetStock }
        if (candidates.isEmpty()) {
            println("입고 권장 대상이 없습니다.")
            return
        }
        println("[입고 권장 목록] (ID / 이름 / 현재/적정(재고율%) / 부족 / 권장입고)")
        candidates.forEach { p ->
            val shortage = p.targetStock - p.currentStock
            val suggest = recommendedReceiveQty(p)
            val ratePct = "%.1f".format(p.currentStock * 100.0 / p.targetStock)
            println("- ${p.id} / ${p.name} / ${p.currentStock}/${p.targetStock}(${ratePct}%) / 부족 ${shortage} / 권장 ${suggest}")
        }
    }

    // =============== 1. 리포트 ===============

    private fun showReport() {
        val list = inv.allProducts()
        println(report.dashboard(list))

        println()
        println("[리포트 후 작업]")
        println("1) 긴급 재고 자동 발주  2) 유통기한 임박 처리(할인등록/폐기)  0) 건너뛰기")
        print("선택> ")
        when (readLine()?.trim()) {
            "1" -> autoReorderFromAlerts()
            "2" -> handleExpiringItems()
            else -> Unit
        }
    }

    // =============== 2. 판매 등록 ===============

    private fun sellFlow() {
        println("[판매 등록]")
        val list = inv.allProducts().filter { it.currentStock > 0 }
        if (list.isEmpty()) return println("판매 가능한 상품이 없습니다.")
        println("판매 가능 상품 (ID / 이름 / 재고 / 가격표시):")
        list.forEach { p ->
            val left = inv.daysLeft(p)
            val rate = pricing.currentDiscountRate(p.id, left)
            val priceDisp = if (rate > 0.0) "${p.price.krw()} → ${discounted(p.price, rate).krw()}" else p.price.krw()
            println("- ${p.id} / ${p.name} / ${p.currentStock}개 / $priceDisp")
        }
        print("상품 ID를 입력하세요: ")
        val id = readLine()?.trim().orEmpty()
        sellFlowWithId(id)
    }

    private fun sellFlowWithId(id: String) {
        print("수량(빈칸=1): ")
        val qty = readLine()?.trim()?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 1
        try {
            inv.sell(id, qty)
            println("판매 등록 완료.")
        } catch (e: Exception) {
            println("오류: ${e.message}")
        }
    }

    // =============== 3. 입고 등록 ===============

    private fun receiveFlow() {
        println("[입고 등록]")
        printReceiveCandidates()
        println()
        println("여러 건을 순서대로 입력하세요. 종료하려면 빈 줄 또는 0.")
        println("입력 예) SNK001,25   또는   SNK001  (수량 생략 시 권장 입고량 사용)")
        while (true) {
            print("상품ID[,수량]> ")
            val line = readLine()?.trim().orEmpty()
            if (line.isBlank() || line == "0") break

            val parts = line.split(',').map { it.trim() }
            val id = parts.getOrNull(0).orEmpty()
            val p = products.findById(id)
            if (p == null) {
                println("상품을 찾을 수 없음: $id"); continue
            }
            val qty = parts.getOrNull(1)?.toIntOrNull() ?: recommendedReceiveQty(p)
            try {
                inv.receive(id, qty)
                println("- ${p.name}: +$qty → 현재 재고 ${p.currentStock + qty}/${p.targetStock}")
            } catch (e: Exception) {
                println("오류: ${e.message}")
            }
        }
        println("입고 등록 종료.")
    }

    private fun receiveFlowWithId(id: String) {
        val p = products.findById(id) ?: return println("상품을 찾을 수 없음")
        print("수량(빈칸=권장 ${recommendedReceiveQty(p)}): ")
        val qty = readLine()?.trim()?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: recommendedReceiveQty(p)
        try {
            inv.receive(id, qty)
            println("입고 등록 완료.")
        } catch (e: Exception) {
            println("오류: ${e.message}")
        }
    }

    // =============== 4. 상품 추가 ===============

    private fun addProductFlow() {
        val allowed = ProductCategory.entries.joinToString("/") { it.name }
        print("이름: "); val name = readLine()?.trim().orEmpty()
        print("분류($allowed 또는 한글): "); val catStr = readLine()?.trim().orEmpty()
        val category = ProductCategory.parse(catStr) ?: return println("분류 입력 오류")
        print("가격: "); val price = readLine()?.trim()?.toIntOrNull() ?: return println("가격 입력 오류")
        print("적정재고: "); val target = readLine()?.trim()?.toIntOrNull() ?: return println("적정재고 입력 오류")
        print("현재재고: "); val cur = readLine()?.trim()?.toIntOrNull() ?: return println("현재재고 입력 오류")
        print("유통기한(YYYY-MM-DD, 없으면 빈칸): "); val expStr = readLine()?.trim()
        val expiry = expStr?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it, dtf) }

        // ID 비우면 자동 생성(카테고리 접두어 + 마지막 번호 + 1)
        print("ID(비우면 자동 생성): "); val idInput = readLine()?.trim().orEmpty()
        val id = if (idInput.isBlank()) genId(category) else idInput

        inv.upsert(Product(id, name, category, price, target, cur, expiry))
        println("상품 추가 완료. 부여된 ID: $id")
    }

    // =============== 5. 상품 수정 ===============

    private fun editProductFlow() {
        println("[상품 수정] 먼저 대상 목록을 확인하세요.")
        printCompactList()
        print("수정할 상품 ID: ")
        val id = readLine()?.trim().orEmpty()
        editProductFlowWithId(id)
    }

    private fun editProductFlowWithId(id: String) {
        val p = products.findById(id) ?: return println("상품을 찾을 수 없음")
        println("ID/분류는 변경할 수 없습니다. (현재 분류: ${p.category.displayName} / ${p.category.name})")
        println("빈칸은 기존값 유지")

        print("이름(${p.name}): ")
        val name = readLine()?.takeIf { !it.isNullOrBlank() } ?: p.name

        print("가격(${p.price}): ")
        val priceStr = readLine()?.trim()
        val price = priceStr?.toIntOrNull() ?: p.price

        print("적정재고(${p.targetStock}): ")
        val tStr = readLine()?.trim()
        val target = tStr?.toIntOrNull() ?: p.targetStock

        print("현재재고(${p.currentStock}): ")
        val cStr = readLine()?.trim()
        val cur = cStr?.toIntOrNull() ?: p.currentStock

        print("유통기한(${p.expiryDate ?: "미관리"}) (YYYY-MM-DD): ")
        val eStr = readLine()?.trim()
        val expiry = if (eStr.isNullOrBlank()) p.expiryDate else LocalDate.parse(eStr, dtf)

        // ✅ ID/분류 고정, 나머지만 갱신
        inv.upsert(
            p.copy(
                name = name,
                price = price,
                targetStock = target,
                currentStock = cur,
                expiryDate = expiry
            )
        )
        println("상품 수정 완료.")
    }


    // =============== 6. 상품 삭제 ===============

    private fun deleteProductFlow() {
        println("[상품 삭제] 먼저 대상 목록을 확인하세요.")
        printCompactList()
        print("삭제할 상품 ID: ")
        val id = readLine()?.trim().orEmpty()
        deleteProductFlowWithId(id)
    }

    private fun deleteProductFlowWithId(id: String) {
        val p = products.findById(id)
        if (p == null) {
            println("상품을 찾을 수 없음: $id")
            return
        }
        print("정말 삭제할까요? (${p.id} ${p.name}) y/N > ")
        when (readLine()?.trim()?.lowercase()) {
            "y", "yes" -> {
                inv.delete(id)
                println("삭제 완료.")
            }
            else -> println("삭제 취소.")
        }
    }

    // =============== 7. 상품 목록/검색(+서브메뉴) ===============

    private fun listProducts() {
        val list = inv.allProducts()
        if (list.isEmpty()) {
            println("등록된 상품이 없습니다.")
            return
        }
        println("[상품 목록] (${list.size}건)")
        println("ID\t|\t이름\t\t\t|\t카테고리\t|\t가격\t|\t적정\t|\t현재\t|\t유통기한")
        list.forEach { p ->
            val price = p.price.krw()
            val exp = renderExpiry(p)
            println("${p.id}\t|\t${p.name}\t|\t${p.category.displayName}\t|\t$price\t|\t${p.targetStock}\t|\t${p.currentStock}\t|\t$exp")
        }

        println()
        print("검색(상품 ID, 빈칸=건너뛰기)> ")
        val q = readLine()?.trim().orEmpty()
        if (q.isBlank()) return

        val p = products.findById(q)
        if (p == null) {
            println("상품을 찾을 수 없음: $q")
            return
        }

        println()
        println("[선택한 상품 상세]")
        println("- ID: ${p.id}")
        println("- 이름: ${p.name}")
        println("- 분류: ${p.category.displayName} (${p.category.name})")
        println("- 가격: ${p.price.krw()}")
        println("- 재고: ${p.currentStock}/${p.targetStock}")
        println("- 유통기한: ${renderExpiry(p)}")

        println()
        println("어떤 것을 도와드릴까요?")
        println("2) 판매 등록  3) 입고 등록  4) 상품 추가  5) 상품 수정  6) 상품 삭제  0) 뒤로")
        print("선택> ")
        when (readLine()?.trim()) {
            "2" -> sellFlowWithId(p.id)
            "3" -> receiveFlowWithId(p.id)
            "4" -> addProductFlow()
            "5" -> editProductFlowWithId(p.id)
            "6" -> deleteProductFlowWithId(p.id)
            else -> Unit
        }
    }

    // =============== 8. 기타 ===============

    // 자동 ID 생성: 카테고리별 접두어 + 3자리 일련번호(기존 최대 + 1)
    private fun genId(category: ProductCategory): String {
        val prefix = when (category) {
            ProductCategory.BEVERAGE -> "BEV"
            ProductCategory.SNACK -> "SNK"
            ProductCategory.CONVENIENCE_MEAL -> "CNV"
            ProductCategory.ICE_CREAM -> "ICE"
            ProductCategory.DAIRY -> "DRY"
            ProductCategory.FROZEN -> "FRZ"
            ProductCategory.HOUSEHOLD -> "HSL"
            ProductCategory.ETC -> "ETC"
        }
        val max = products.findAll().mapNotNull {
            Regex("^$prefix(\\d{3})$").find(it.id)?.groupValues?.get(1)?.toInt()
        }.maxOrNull() ?: 0
        return "%s%03d".format(prefix, max + 1)
    }

    // 리포트 후 작업: 자동 발주 / 임박 처리
    private fun autoReorderFromAlerts(threshold: Double = 0.30) {
        val list = inv.allProducts()
        val targets = list.filter { inv.needsReorder(it, threshold) }
        if (targets.isEmpty()) {
            println("긴급 재고 대상이 없습니다.")
            return
        }
        println("다음 상품에 대해 권장 발주량으로 자동 발주(즉시 입고)합니다.")
        targets.forEach { p ->
            val suggest = (p.targetStock - p.currentStock).coerceAtLeast(1)
            inv.receive(p.id, suggest)
            println("- ${p.name}: +$suggest → 현재 재고 ${p.currentStock + suggest}/${p.targetStock}")
        }
        println("자동 발주 완료.")
    }

    private fun handleExpiringItems(warnDays: Int = 3) {
        val soon = inv.expiringSoon(inv.allProducts(), warnDays)
        if (soon.isEmpty()) {
            println("유통기한 임박 상품 없음")
            return
        }
        println("[임박 상품 요약] (ID / 이름 / 재고 / 유통기한 D-day / 권장 할인)")
        soon.forEach { p ->
            val left = inv.daysLeft(p) ?: 0
            val rec = (pricing.discountRateByDaysLeft(left) * 100).toInt()
            val dStr = "${p.expiryDate} (D-$left)"
            println("- ${p.id} / ${p.name} / ${p.currentStock}개 / $dStr / 권장 ${rec}%")
        }
        println()
        println("작업 선택: 1) 할인 등록  2) 폐기 처리  0) 뒤로")
        print("선택> ")
        when (readLine()?.trim()) {
            "1" -> registerDiscountsLoop(warnDays)
            "2" -> disposeLoop()
            else -> Unit
        }
    }

    // 할인 등록/폐기 루프(이전 버전과 동일)
    private fun registerDiscountsLoop(warnDays: Int) {
        println("할인 등록을 진행합니다.")
        println("입력 예시) CNV001,40   또는   CNV001   (권장 할인 자동 적용)")
        println("여러 건을 순서대로 입력하세요. 종료하려면 빈 줄 또는 0 입력.")
        while (true) {
            print("상품ID[,할인%]> ")
            val line = readLine()?.trim().orEmpty()
            if (line.isBlank() || line == "0") break

            val parts = line.split(',').map { it.trim() }
            val id = parts.getOrNull(0).orEmpty()
            val p = products.findById(id)
            if (p == null) {
                println("상품을 찾을 수 없음: $id"); continue
            }
            val percent = parts.getOrNull(1)?.toIntOrNull() ?: run {
                val left = inv.daysLeft(p) ?: warnDays
                (pricing.discountRateByDaysLeft(left) * 100).toInt()
            }
            try {
                pricing.setManualDiscount(id, percent)
                val toPrice = discounted(p.price, percent / 100.0)
                println("- ${p.name}: 수동 할인 ${percent}% 등록 (${p.price.krw()} → ${toPrice.krw()})")
            } catch (e: Exception) {
                println("오류: ${e.message}")
            }
        }
        println("할인 등록 종료.")
    }

    private fun disposeLoop() {
        println("폐기 처리를 진행합니다.")
        println("입력 예시) CNV001,2  또는  CNV001   (수량 생략 시 1개)")
        println("여러 건을 순서대로 입력하세요. 종료하려면 빈 줄 또는 0 입력.")
        while (true) {
            print("상품ID, [수량]> ")
            val line = readLine()?.trim().orEmpty()
            if (line.isBlank() || line == "0") break

            val parts = line.split(',').map { it.trim() }
            val id = parts.getOrNull(0).orEmpty()
            val qty = parts.getOrNull(1)?.toIntOrNull() ?: 1
            try {
                inv.dispose(id, qty)
                println("- $id: 폐기 ${qty}개 완료")
            } catch (e: Exception) {
                println("오류: ${e.message}")
            }
        }
        println("폐기 처리 종료.")
    }
}