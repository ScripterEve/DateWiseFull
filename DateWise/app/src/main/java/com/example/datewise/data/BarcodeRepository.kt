package com.example.datewise.data

import com.example.datewise.data.api.OpenFoodFactsApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class ProductInfo(
    val name: String,
    val brand: String,
    val description: String,
    val barcode: String
)

class BarcodeRepository {
    private val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    suspend fun lookupBarcode(barcode: String): Result<ProductInfo> {
        return try {
            val response = api.getProduct(barcode)
            if (response.status == 1 && response.product != null) {
                val product = response.product
                val name = product.productName ?: "Unknown Product"
                val brand = product.brands ?: ""
                val quantity = product.quantity ?: ""
                val description = if (brand.isNotEmpty() && quantity.isNotEmpty()) {
                    "$brand • $quantity"
                } else if (brand.isNotEmpty()) {
                    brand
                } else {
                    quantity
                }

                Result.success(
                    ProductInfo(
                        name = name,
                        brand = brand,
                        description = description,
                        barcode = barcode
                    )
                )
            } else {
                Result.failure(Exception("Product not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
