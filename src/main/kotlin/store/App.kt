package store

fun main() {
    // 저장소
    val productRepo = InMemoryProductRepository()
    val salesRepo   = InMemorySalesRepository()

    // 서비스
    val pricing  = PricingService()
    val inv      = InventoryService(SystemClock, productRepo, salesRepo, pricing)
    val analysis = AnalysisService(SystemClock, productRepo, salesRepo)
    val report   = ReportService(inv, analysis, pricing)

    // 샘플 데이터 적재
    seedData(productRepo, salesRepo)

    // 콘솔 앱 실행 (🔸 pricing 주입)
    ConsoleApp(productRepo, inv, analysis, report, pricing).run()
}

private fun seedData(products: ProductRepository, sales: SalesRepository) {
    val today = SystemClock.today()

    // ID 규칙 적용: 카테고리 접두어 + 3자리 일련번호
    val cola   = Product("BEV001", "콜라 500ml", ProductCategory.BEVERAGE,         1800, 30, 10, today.plusDays(5))
    val coffee = Product("BEV002", "아이스 커피",  ProductCategory.BEVERAGE,         2500, 25,  8, today.plusDays(10))
    val chips  = Product("SNK001", "새우깡",       ProductCategory.SNACK,            1500, 30,  5) // 과자류는 유통기한 미관리 예시(null)
    val bar    = Product("SNK002", "초코바",       ProductCategory.SNACK,            1200, 20, 12)
    val lunch  = Product("CNV001", "김치찌개 도시락", ProductCategory.CONVENIENCE_MEAL, 4800, 20,  3, today.plusDays(1))
    val onigiri= Product("CNV002", "삼각김밥 참치", ProductCategory.CONVENIENCE_MEAL, 1800, 25,  9, today.plusDays(2))
    val cone   = Product("ICE001", "월드콘",       ProductCategory.ICE_CREAM,        2000, 15,  7, today.plusDays(20))
    val milk   = Product("DRY001", "흰우유 1L",    ProductCategory.DAIRY,            2600, 18,  6, today.plusDays(3))
    val dumpling=Product("FRZ001","냉동만두 1kg",  ProductCategory.FROZEN,           8900, 22, 14, today.plusDays(60))
    val soap   = Product("HSL001", "핸드솝",       ProductCategory.HOUSEHOLD,        3500, 16,  4)

    // 저장
    listOf(cola, coffee, chips, bar, lunch, onigiri, cone, milk, dumpling, soap).forEach(products::save)

    // 최근 7일 판매 이력 예시(베스트셀러용)
    repeat(5) { sales.record(Sale(productId = cola.id,   qty = 1, priceAtSale = cola.price)) }
    repeat(2) { sales.record(Sale(productId = chips.id,  qty = 1, priceAtSale = chips.price)) }
    repeat(3) { sales.record(Sale(productId = coffee.id, qty = 1, priceAtSale = coffee.price)) }
    repeat(1) { sales.record(Sale(productId = cone.id,   qty = 1, priceAtSale = cone.price)) }
    // 도시락/삼각김밥은 판매 적음(임박/할인/폐기 후보)
}