package store

/*
 * 애플리케이션 진입점.
 * - 의존성 조립(DI)을 한 곳에서 처리
 * - 간단한 시드 데이터 넣고 콘솔 앱 실행
 */
fun main() {
    // 저장소(메모리 구현체)
    val productRepo = InMemoryProductRepository()
    val salesRepo   = InMemorySalesRepository()

    // 서비스 계층
    val pricing  = PricingService()
    val inv      = InventoryService(SystemClock, productRepo, salesRepo, pricing)
    val analysis = AnalysisService(SystemClock, productRepo, salesRepo)
    val report   = ReportService(inv, analysis, pricing)

    // 샘플 데이터 시드
    Bootstrap.seed(productRepo, salesRepo)

    // 콘솔 UI 실행
    ConsoleApp(productRepo, inv, analysis, report).run()
}

/** 간단한 시드 데이터 모음 **/
object Bootstrap {
    fun seed(products: ProductRepository, sales: SalesRepository) {
        // 편의상 id는 문자열로
        val cola  = Product(id="P001", name="콜라",   category=ProductCategory.BEVERAGE, price=1800, targetStock=30, currentStock=10)
        val chips = Product(id="P002", name="새우깡", category=ProductCategory.SNACK,    price=1500, targetStock=30, currentStock=5)
        val lunch = Product(id="P003", name="김치찌개 도시락", category=ProductCategory.FOOD, price=4800, targetStock=20, currentStock=3)

        products.save(cola)
        products.save(chips)
        products.save(lunch)

        // 최근 7일 판매 이력 예시
        repeat(5) { sales.record(Sale(productId="P001", qty=1, priceAtSale=1800)) } // 콜라 5개
        repeat(2) { sales.record(Sale(productId="P002", qty=1, priceAtSale=1500)) } // 새우깡 2개
        // 도시락은 판매 적음(죽은 재고 후보)
    }
}