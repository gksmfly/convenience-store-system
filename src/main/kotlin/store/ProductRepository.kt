package store

/* 상품 저장소 인터페이스(교체 가능성 고려) */
interface ProductRepository {
    fun findAll(): List<Product>
    fun findById(id: String): Product?
    fun save(product: Product)
    fun delete(id: String)
}

/* 메모리 구현체(Phase1/2 데모에 충분) */
class InMemoryProductRepository : ProductRepository {
    private val data = linkedMapOf<String, Product>()

    override fun findAll(): List<Product> = data.values.toList()
    override fun findById(id: String): Product? = data[id]
    override fun save(product: Product) { data[product.id] = product }
    override fun delete(id: String) { data.remove(id) }
}