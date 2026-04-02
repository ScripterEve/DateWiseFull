package com.example.datewisepos.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.datewisepos.data.local.AppDatabase
import com.example.datewisepos.data.local.ExpiryRecord
import com.example.datewisepos.data.local.Product
import com.example.datewisepos.data.remote.RetrofitClient
import com.example.datewisepos.data.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProductWithExpiry(
    val product: Product,
    val latestExpiry: ExpiryRecord? = null
)

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = ProductRepository(db, RetrofitClient.api)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<ProductWithExpiry>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllProducts()
            } else {
                repository.searchProducts(query)
            }
        }
        .map { products ->
            products.map { product ->
                val latestExpiry = repository.getLatestExpiry(product.id)
                ProductWithExpiry(product, latestExpiry)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            repository.deleteProduct(id)
        }
    }
}
