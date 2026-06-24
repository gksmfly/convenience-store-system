# Convenience Store System — Smart Inventory Management for a 24/7 Campus Store

> **TL;DR**: A console-based inventory management system for a school convenience store, automating expiry-driven discounts, reorder recommendations, and sales analytics using Kotlin.

---

## Problem Statement

A 24-hour unmanned campus convenience store faces three recurring inefficiencies:

- No real-time visibility into inventory levels, leading to stockouts or overstock going unnoticed
- Manual tracking of expiring products results in waste and inconsistent pricing
- No data-driven sales reporting makes restocking decisions purely intuition-based

---

## Approach

- **Rule-based discount pipeline**: Expiry proximity (D-3/D-2/D-1/D-0) maps directly to discount tiers (0%/30%/50%/70%); manual discounts override auto-discounts when set
- **Smart reorder formula**: `recommended_qty = max(0, target_stock - current_stock) + safety_stock (10% of target)` — surfaces actionable numbers rather than just alerts
- **Service-layer separation**: Inventory, Pricing, Analysis, and Report logic are each in dedicated service classes to keep business rules isolated from console I/O
- **Testable time abstraction**: A `Clock` interface wraps `LocalDate.now()` so expiry calculations can be tested without manipulating system time

---

## Key Results

| Feature | Detail |
|---------|--------|
| Discount tiers | D-3: 0%, D-2: 30%, D-1: 50%, D-0: 70% |
| Alert threshold | Stock ratio < 30% triggers urgent warning |
| Reorder logic | Auto-calculates recommended quantity per product |
| Report output | Daily/weekly sales, TOP-5 bestsellers, turnover rate, overstock list |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2.0 |
| Runtime | JVM (Java 24) |
| Build | Gradle 8.x (Wrapper included) |
| Dependencies | Kotlin stdlib only |

---

## Project Structure

```
src/main/kotlin/store/
├── App.kt               # Entry point — wires repositories and services
├── ConsoleApp.kt        # Console UI, menu routing
├── Product.kt           # Product data class
├── Sale.kt              # Sale transaction data class
├── Enums.kt             # ProductCategory, InventoryStatus
├── Clock.kt             # Time abstraction (testable)
├── Money.kt             # Monetary value model
├── *Repository.kt       # ProductRepository, SalesRepository
└── *Service.kt          # InventoryService, PricingService, AnalysisService, ReportService
```

---

## Getting Started

```bash
# Build
./gradlew clean build

# Run
java -jar build/libs/convenience-store-system-1.0.0.jar
```

> If your local JDK version is below 24, lower `jvmToolchain(24)` in `build.gradle.kts` accordingly.

---

## Limitations & Future Work

- Data is in-memory only — all state is lost on program exit; a SQLite or H2 integration would add persistence
- Discount and reorder policies are hardcoded; extracting them to a Strategy pattern would allow per-store configuration
- Sales reporting could be extended to a web dashboard for non-technical store operators

---

## License

MIT License — see [LICENSE](LICENSE) for details.

---

## Author

**Seoyeon Kim** | Undergraduate Student  
[GitHub](https://github.com/gksmfly) · [Email](mailto:gimhaneul24@gmail.com)
