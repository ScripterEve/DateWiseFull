package com.example.datewisepos.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.datewisepos.data.local.AppDatabase
import com.example.datewisepos.data.local.ExpiryRecord
import com.example.datewisepos.data.local.Product
import com.example.datewisepos.data.remote.RetrofitClient
import com.example.datewisepos.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProductDetailState(
    val product: Product? = null,
    val expiryRecords: List<ExpiryRecord> = emptyList(),
    val isLoading: Boolean = true
)

class ProductDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = ProductRepository(db, RetrofitClient.api)

    private val _state = MutableStateFlow(ProductDetailState())
    val state: StateFlow<ProductDetailState> = _state.asStateFlow()

    fun loadProduct(productId: Long) {
        viewModelScope.launch {
            val product = repository.getProductById(productId)
            _state.value = _state.value.copy(product = product, isLoading = false)
        }

        viewModelScope.launch {
            repository.getExpiryRecords(productId).collect { records ->
                _state.value = _state.value.copy(expiryRecords = records)
            }
        }
    }

    fun addExpiryRecord(productId: Long, expiryDateMillis: Long) {
        viewModelScope.launch {
            repository.insertExpiryRecord(
                ExpiryRecord(
                    productId = productId,
                    expiryDate = expiryDateMillis
                )
            )
        }
    }

    fun deleteExpiryRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteExpiryRecord(id)
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
        }
    }
}
