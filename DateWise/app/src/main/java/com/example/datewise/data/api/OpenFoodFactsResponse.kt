package com.example.datewise.data.api

import com.google.gson.annotations.SerializedName

data class OpenFoodFactsResponse(
    @SerializedName("status") val status: Int,
    @SerializedName("product") val product: OFFProduct?
)

data class OFFProduct(
    @SerializedName("product_name") val productName: String?,
    @SerializedName("brands") val brands: String?,
    @SerializedName("quantity") val quantity: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("categories") val categories: String?
)
