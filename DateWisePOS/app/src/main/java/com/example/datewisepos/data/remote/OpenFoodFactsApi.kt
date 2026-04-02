package com.example.datewisepos.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {

    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): OpenFoodFactsResponse
}

data class OpenFoodFactsResponse(
    val status: Int,
    val product: OpenFoodFactsProduct?
)

data class OpenFoodFactsProduct(
    val product_name: String?,
    val brands: String?,
    val quantity: String?,
    val image_url: String?
)
