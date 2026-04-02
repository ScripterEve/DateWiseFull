package com.example.datewisepos.ui.batchticket

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.datewisepos.util.BatchTicketGenerator
import com.example.datewisepos.util.SunmiPrinterHelper
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Represents one scanned ticket item with full product info.
 */
data class ScannedTicketItem(
    val barcode: String,
    val expiryDate: String,   // ddMMyyyy format
    val name: String = "",
    val brand: String = "",
    val quantity: String = ""
)

data class BatchTicketUiState(
    val scannedItems: List<ScannedTicketItem> = emptyList(),
    val pendingBarcode: String? = null,
    val combinedTicketBitmap: Bitmap? = null,
    val showPreview: Boolean = false,
    val isPrinting: Boolean = false,
    val printResult: BatchPrintResult? = null,
    val lastScanInfo: String? = null
)

sealed class BatchPrintResult {
    object Success : BatchPrintResult()
    data class Error(val message: String) : BatchPrintResult()
}

class BatchTicketViewModel(application: Application) : AndroidViewModel(application) {

    private val printerHelper = SunmiPrinterHelper(application)

    private val _uiState = MutableStateFlow(BatchTicketUiState())
    val uiState: StateFlow<BatchTicketUiState> = _uiState.asStateFlow()

    /**
     * Called when the camera detects a code.
     *
     * DataMatrix tickets encode: "name|brand|barcode|quantity|ddMMyyyy"
     * So scanning just the DataMatrix is enough to create a complete item.
     * Scanning the 1D barcode alone stores it as pending.
     */
    fun onCodeScanned(rawValue: String, format: Int) {
        val state = _uiState.value

        if (format == Barcode.FORMAT_DATA_MATRIX) {
            // Try to parse the DataMatrix content: "name|brand|barcode|quantity|expiry"
            val parts = rawValue.split("|")
            if (parts.size == 5) {
                val dmName = parts[0]
                val dmBrand = parts[1]
                val dmBarcode = parts[2]
                val dmQuantity = parts[3]
                val dmExpiry = parts[4]

                // Auto-create complete item from single DataMatrix scan
                val newItem = ScannedTicketItem(
                    barcode = dmBarcode,
                    expiryDate = dmExpiry,
                    name = dmName,
                    brand = dmBrand,
                    quantity = dmQuantity
                )
                _uiState.value = state.copy(
                    scannedItems = state.scannedItems + newItem,
                    pendingBarcode = null,
                    combinedTicketBitmap = null,
                    showPreview = false,
                    lastScanInfo = "✓ Added: $dmName ($dmBarcode, exp ${formatExpiry(dmExpiry)})"
                )
            } else {
                // Fallback: old-format DataMatrix with just expiry date
                // Pair with pending barcode if available
                val pending = state.pendingBarcode
                if (pending != null) {
                    val newItem = ScannedTicketItem(barcode = pending, expiryDate = rawValue)
                    _uiState.value = state.copy(
                        scannedItems = state.scannedItems + newItem,
                        pendingBarcode = null,
                        combinedTicketBitmap = null,
                        showPreview = false,
                        lastScanInfo = "✓ Added: $pending (exp ${formatExpiry(rawValue)})"
                    )
                } else {
                    _uiState.value = state.copy(
                        lastScanInfo = "Expiry scanned. Now scan the barcode..."
                    )
                }
            }
        } else {
            // 1D barcode (EAN, UPC, Code128, etc.)
            // Don't block duplicate barcodes here — different expiry dates are valid.
            // The DataMatrix path will check for exact duplicates (barcode + expiry).

            // Store as pending — we'll pair when the DataMatrix is scanned,
            // but most likely the DataMatrix already contains the barcode too
            _uiState.value = state.copy(
                pendingBarcode = rawValue,
                lastScanInfo = "Barcode: $rawValue — now scan the DataMatrix..."
            )
        }
    }

    fun removeItem(index: Int) {
        val state = _uiState.value
        if (index in state.scannedItems.indices) {
            _uiState.value = state.copy(
                scannedItems = state.scannedItems.toMutableList().apply { removeAt(index) },
                combinedTicketBitmap = null,
                showPreview = false
            )
        }
    }

    fun generateCombinedTicket() {
        val items = _uiState.value.scannedItems
        if (items.isEmpty()) return

        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                BatchTicketGenerator.generateBatchTicket(items)
            }
            _uiState.value = _uiState.value.copy(
                combinedTicketBitmap = bitmap,
                showPreview = true
            )
        }
    }

    fun printTicket() {
        val bitmap = _uiState.value.combinedTicketBitmap ?: return
        _uiState.value = _uiState.value.copy(isPrinting = true, printResult = null)

        viewModelScope.launch {
            try {
                printerHelper.printBitmap(bitmap)
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printResult = BatchPrintResult.Success
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPrinting = false,
                    printResult = BatchPrintResult.Error(e.message ?: "Print failed")
                )
            }
        }
    }

    fun clearPrintResult() {
        _uiState.value = _uiState.value.copy(printResult = null)
    }

    fun dismissPreview() {
        _uiState.value = _uiState.value.copy(showPreview = false)
    }

    fun clearAll() {
        _uiState.value = BatchTicketUiState()
    }

    override fun onCleared() {
        super.onCleared()
        printerHelper.disconnect()
    }

    private fun formatExpiry(exp: String): String {
        return if (exp.length == 8) {
            "${exp.substring(0, 2)}-${exp.substring(2, 4)}-${exp.substring(4, 8)}"
        } else exp
    }
}
