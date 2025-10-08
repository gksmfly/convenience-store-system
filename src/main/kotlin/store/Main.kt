package store

// 실행 진입점
fun main() {
    // 샘플 데이터 기반으로 InventoryManager 생성
    val manager = InventoryManager.sample()

    // 일일 리포트 전체 실행 (모든 Phase 기능 포함)
    manager.runDailyReport()
}