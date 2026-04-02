package com.example.datewise.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.datewise.data.BarcodeRepository
import com.example.datewise.data.DateWiseDatabase
import com.example.datewise.data.Product
import com.example.datewise.data.ProductCategory
import com.example.datewise.data.ProductInfo
import com.example.datewise.data.ShoppingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DateWiseDatabase.getDatabase(application)
    private val productDao = database.productDao()
    private val shoppingDao = database.shoppingDao()
    private val donatedItemDao = database.donatedItemDao()
    private val barcodeRepository = BarcodeRepository()
    private val prefs = application.getSharedPreferences("datewise_prefs", android.content.Context.MODE_PRIVATE)

    // Products from Room (reactive)
    val products: StateFlow<List<Product>> = productDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products expiring in <= 7 days
    val expiringProducts: StateFlow<List<Product>> = products
        .map { list ->
            val today = LocalDate.now()
            list.filter { product ->
                val daysUntil = ChronoUnit.DAYS.between(today, product.expiryDate)
                daysUntil <= 7L
            }.sortedBy { it.expiryDate }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shopping items from Room (reactive)
    val shoppingItems: StateFlow<List<ShoppingItem>> = shoppingDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Donated items from Room (reactive)
    val donatedItems: StateFlow<List<com.example.datewise.data.DonatedItem>> = donatedItemDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feature toggles
    private val _isDonationsEnabled = MutableStateFlow(prefs.getBoolean("donations_enabled", false))
    val isDonationsEnabled: StateFlow<Boolean> = _isDonationsEnabled.asStateFlow()

    // UI state
    private val _selectedCategory = MutableStateFlow(ProductCategory.FRIDGE)
    val selectedCategory: StateFlow<ProductCategory> = _selectedCategory.asStateFlow()

    // Barcode lookup state
    private val _barcodeLookupState = MutableStateFlow<BarcodeLookupState>(BarcodeLookupState.Idle)
    val barcodeLookupState: StateFlow<BarcodeLookupState> = _barcodeLookupState.asStateFlow()

    // Shopping barcode lookup state (separate from product barcode)
    private val _shoppingBarcodeLookupState = MutableStateFlow<BarcodeLookupState>(BarcodeLookupState.Idle)
    val shoppingBarcodeLookupState: StateFlow<BarcodeLookupState> = _shoppingBarcodeLookupState.asStateFlow()

    // Pending batch items for sequential scanning
    private val _pendingBatchItems = MutableStateFlow<List<com.example.datewise.ui.screens.ReceiptData>>(emptyList())
    val pendingBatchItems: StateFlow<List<com.example.datewise.ui.screens.ReceiptData>> = _pendingBatchItems.asStateFlow()

    fun setPendingBatchItems(items: List<com.example.datewise.ui.screens.ReceiptData>) {
        _pendingBatchItems.value = items
    }

    fun popNextBatchItem(): com.example.datewise.ui.screens.ReceiptData? {
        val currentList = _pendingBatchItems.value
        if (currentList.isEmpty()) return null
        val nextItem = currentList.first()
        _pendingBatchItems.value = currentList.drop(1)
        return nextItem
    }

    fun selectCategory(category: ProductCategory) {
        _selectedCategory.value = category
    }

    // Product operations (database-backed)
    fun addProduct(product: Product) {
        viewModelScope.launch {
            productDao.insert(product)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            productDao.update(product)
        }
    }

    fun removeProduct(productId: Int) {
        viewModelScope.launch {
            productDao.deleteById(productId)
        }
    }

    fun donateProduct(product: Product) {
        viewModelScope.launch {
            productDao.deleteById(product.id)
            donatedItemDao.insert(
                com.example.datewise.data.DonatedItem(
                    name = product.name,
                    barcode = product.barcode,
                    description = product.description,
                    category = product.category
                )
            )
        }
    }

    fun toggleDonationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("donations_enabled", enabled).apply()
        _isDonationsEnabled.value = enabled
    }

    suspend fun getProductById(productId: Int): Product? {
        return productDao.getById(productId)
    }

    // Shopping list operations (database-backed)
    fun addShoppingItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingDao.insert(item)
        }
    }

    fun removeShoppingItem(itemId: Int) {
        viewModelScope.launch {
            shoppingDao.deleteById(itemId)
        }
    }

    fun toggleShoppingItemChecked(itemId: Int) {
        viewModelScope.launch {
            shoppingDao.toggleChecked(itemId)
        }
    }

    fun clearCheckedShoppingItems() {
        viewModelScope.launch {
            shoppingDao.clearChecked()
        }
    }

    // Barcode lookup
    fun lookupBarcode(barcode: String) {
        _barcodeLookupState.value = BarcodeLookupState.Loading
        viewModelScope.launch {
            val result = barcodeRepository.lookupBarcode(barcode)
            _barcodeLookupState.value = result.fold(
                onSuccess = { BarcodeLookupState.Found(it) },
                onFailure = { BarcodeLookupState.NotFound(barcode) }
            )
        }
    }

    fun resetBarcodeLookup() {
        _barcodeLookupState.value = BarcodeLookupState.Idle
    }

    // Shopping barcode lookup
    fun lookupBarcodeForShopping(barcode: String) {
        _shoppingBarcodeLookupState.value = BarcodeLookupState.Loading
        viewModelScope.launch {
            val result = barcodeRepository.lookupBarcode(barcode)
            _shoppingBarcodeLookupState.value = result.fold(
                onSuccess = { BarcodeLookupState.Found(it) },
                onFailure = { BarcodeLookupState.NotFound(barcode) }
            )
        }
    }

    fun resetShoppingBarcodeLookup() {
        _shoppingBarcodeLookupState.value = BarcodeLookupState.Idle
    }
}

sealed class BarcodeLookupState {
    object Idle : BarcodeLookupState()
    object Loading : BarcodeLookupState()
    data class Found(val productInfo: ProductInfo) : BarcodeLookupState()
    data class NotFound(val barcode: String) : BarcodeLookupState()
}