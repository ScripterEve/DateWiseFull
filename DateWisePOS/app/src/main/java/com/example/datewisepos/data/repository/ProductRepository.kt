package com.example.datewisepos.data.repository

import com.example.datewisepos.data.local.AppDatabase
import com.example.datewisepos.data.local.ExpiryRecord
import com.example.datewisepos.data.local.Product
import com.example.datewisepos.data.remote.OpenFoodFactsApi
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val db: AppDatabase,
    private val api: OpenFoodFactsApi
) {
    private val productDao = db.productDao()
    private val expiryRecordDao = db.expiryRecordDao()

    // Products
    fun getAllProducts(): Flow<List<Product>> = productDao.getAll()

    fun searchProducts(query: String): Flow<List<Product>> = productDao.search(query)

    suspend fun getProductByBarcode(barcode: String): Product? = productDao.getByBarcode(barcode)

    suspend fun getProductById(id: Long): Product? = productDao.getById(id)

    suspend fun insertProduct(product: Product): Long = productDao.insert(product)

    suspend fun updateProduct(product: Product) = productDao.update(product)

    suspend fun deleteProduct(id: Long) = productDao.deleteById(id)

    // Expiry Records
    fun getExpiryRecords(productId: Long): Flow<List<ExpiryRecord>> =
        expiryRecordDao.getForProduct(productId)

    suspend fun getLatestExpiry(productId: Long): ExpiryRecord? =
        expiryRecordDao.getLatestForProduct(productId)

    suspend fun insertExpiryRecord(record: ExpiryRecord): Long =
        expiryRecordDao.insert(record)

    suspend fun deleteExpiryRecord(id: Long) = expiryRecordDao.deleteById(id)

    fun getAllExpiryRecords(): Flow<List<ExpiryRecord>> = expiryRecordDao.getAll()

    // Remote API
    suspend fun lookupProduct(barcode: String): ProductLookupResult {
        return try {
            val response = api.getProduct(barcode)
            if (response.status == 1 && response.product != null) {
                val p = response.product
                ProductLookupResult.Found(
                    name = p.product_name ?: "",
                    brand = p.brands ?: "",
                    quantity = p.quantity ?: "",
                    imageUrl = p.image_url
                )
            } else {
                ProductLookupResult.NotFound
            }
        } catch (e: Exception) {
            ProductLookupResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class ProductLookupResult {
    data class Found(
        val name: String,
        val brand: String,
        val quantity: String,
        val imageUrl: String?
    ) : ProductLookupResult()

    object NotFound : ProductLookupResult()
    data class Error(val message: String) : ProductLookupResult()
}
