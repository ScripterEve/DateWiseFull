package com.example.datewisepos.ui.ticket

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.datewisepos.data.local.AppDatabase
import com.example.datewisepos.data.local.Product
import com.example.datewisepos.data.remote.RetrofitClient
import com.example.datewisepos.data.repository.ProductRepository
import com.example.datewisepos.util.SunmiPrinterHelper
import com.example.datewisepos.util.TicketGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TicketUiState(
    val product: Product? = null,
    val expiryDateMillis: Long = 0,
    val ticketBitmap: Bitmap? = null,
    val isGenerating: Boolean = true,
    val isPrinting: Boolean = false,
    val printResult: PrintResult? = null,
    val errorMessage: String? = null
)

sealed class PrintResult {
    object Success : PrintResult()
    data class Error(val message: String) : PrintResult()
}

class TicketViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = ProductRepository(db, RetrofitClient.api)
    private val printerHelper = SunmiPrinterHelper(application)

    private val _uiState = MutableStateFlow(TicketUiState())
    val uiState: StateFlow<TicketUiState> = _uiState.asStateFlow()

    fun generateTicket(productId: Long, expiryDateMillis: Long) {
        viewModelScope.launch {
            val product = repository.getProductById(productId)
            _uiState.value = _uiState.value.copy(
                product = product,
                expiryDateMillis = expiryDateMillis
            )

            if (product != null) {
                try {
                    val bitmap = withContext(Dispatchers.Default) {
                        TicketGenerator.generateTicket(
                            barcode = product.barcode,
                            expiryDateMillis = expiryDateMillis,
                            productName = product.name,
                            productBrand = product.brand,
                            productQuantity = product.quantity
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        ticketBitmap = bitmap,
                        isGenerating = false
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        errorMessage = "Failed to generate ticket: ${e.message}"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = "Product not found"
                )
            }
        }
    }

    fun printTicket() {
        val bitmap = _uiState.value.ticketBitmap ?: return
        _uiState.value = _uiState.value.copy(isPrinting = true, printResult = null)

        viewModelScope.launch {
            try {
                printerHelper.printBitmap(bitmap)
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printResult = PrintResult.Success
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printResult = PrintResult.Error(e.message ?: "Print failed")
                )
            }
        }
    }

    fun clearPrintResult() {
        _uiState.value = _uiState.value.copy(printResult = null)
    }

    override fun onCleared() {
        super.onCleared()
        printerHelper.disconnect()
    }
}
