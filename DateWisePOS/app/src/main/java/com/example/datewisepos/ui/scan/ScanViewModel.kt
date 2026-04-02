package com.example.datewisepos.ui.scan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.datewisepos.data.local.AppDatabase
import com.example.datewisepos.data.local.Product
import com.example.datewisepos.data.remote.RetrofitClient
import com.example.datewisepos.data.repository.ProductLookupResult
import com.example.datewisepos.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanUiState(
    val scannedBarcode: String = "",
    val lookupState: LookupState = LookupState.Idle,
    val productName: String = "",
    val productBrand: String = "",
    val productQuantity: String = "",
    val productImageUrl: String? = null,
    val isSaving: Boolean = false,
    val savedProductId: Long? = null,
    val existingProduct: Product? = null
)

enum class LookupState {
    Idle, Loading, Found, NotFound, Error, AlreadyExists
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = ProductRepository(db, RetrofitClient.api)

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onBarcodeScanned(barcode: String) {
        _uiState.value = _uiState.value.copy(
            scannedBarcode = barcode,
            lookupState = LookupState.Loading
        )
        viewModelScope.launch {
            // Check if product already exists locally
            val existing = repository.getProductByBarcode(barcode)
            if (existing != null) {
                _uiState.value = _uiState.value.copy(
                    lookupState = LookupState.AlreadyExists,
                    existingProduct = existing,
                    productName = existing.name,
                    productBrand = existing.brand,
                    productQuantity = existing.quantity,
                    productImageUrl = existing.imageUrl
                )
                return@launch
            }

            // Lookup from Open Food Facts
            when (val result = repository.lookupProduct(barcode)) {
                is ProductLookupResult.Found -> {
                    _uiState.value = _uiState.value.copy(
                        lookupState = LookupState.Found,
                        productName = result.name,
                        productBrand = result.brand,
                        productQuantity = result.quantity,
                        productImageUrl = result.imageUrl
                    )
                }
                is ProductLookupResult.NotFound -> {
                    _uiState.value = _uiState.value.copy(
                        lookupState = LookupState.NotFound
                    )
                }
                is ProductLookupResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        lookupState = LookupState.Error
                    )
                }
            }
        }
    }

    fun updateBarcode(barcode: String) {
        _uiState.value = _uiState.value.copy(scannedBarcode = barcode)
    }

    fun lookupBarcode() {
        val barcode = _uiState.value.scannedBarcode.trim()
        if (barcode.isBlank()) return
        onBarcodeScanned(barcode)
    }

    fun initWithBarcode(barcode: String) {
        if (barcode.isNotBlank() && _uiState.value.scannedBarcode != barcode) {
            onBarcodeScanned(barcode)
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(productName = name)
    }

    fun updateBrand(brand: String) {
        _uiState.value = _uiState.value.copy(productBrand = brand)
    }

    fun updateQuantity(quantity: String) {
        _uiState.value = _uiState.value.copy(productQuantity = quantity)
    }

    fun saveProduct() {
        val state = _uiState.value
        if (state.productName.isBlank() || state.scannedBarcode.isBlank()) return

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val product = Product(
                barcode = state.scannedBarcode,
                name = state.productName,
                brand = state.productBrand,
                quantity = state.productQuantity,
                imageUrl = state.productImageUrl
            )
            val id = repository.insertProduct(product)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                savedProductId = id
            )
        }
    }

    fun resetState() {
        _uiState.value = ScanUiState()
    }
}
