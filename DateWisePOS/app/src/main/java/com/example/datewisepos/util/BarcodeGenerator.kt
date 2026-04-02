package com.example.datewisepos.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.datamatrix.DataMatrixWriter
import java.text.SimpleDateFormat
import java.util.*

object BarcodeGenerator {

    /**
     * Generate a standard barcode (Code 128) as a Bitmap.
     * Used for the product barcode on the ticket.
     */
    fun generateBarcode(
        data: String,
        width: Int = 600,
        height: Int = 200
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 2
        )
        val writer = MultiFormatWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.CODE_128, width, height, hints)
        return bitMatrixToBitmap(bitMatrix)
    }

    /**
     * Generate a Data Matrix 2D barcode encoding product info + expiry date.
     * Format: "name|brand|barcode|quantity|ddMMyyyy"
     */
    fun generateExpiryDataMatrix(
        expiryDateMillis: Long,
        barcode: String = "",
        name: String = "",
        brand: String = "",
        quantity: String = "",
        size: Int = 200
    ): Bitmap {
        val dateFormat = SimpleDateFormat("ddMMyyyy", Locale.getDefault())
        val expiryString = dateFormat.format(Date(expiryDateMillis))
        val dataString = "$name|$brand|$barcode|$quantity|$expiryString"

        val hints = mapOf(
            EncodeHintType.MARGIN to 0,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val writer = DataMatrixWriter()
        val bitMatrix = writer.encode(dataString, BarcodeFormat.DATA_MATRIX, size, size, hints)
        return bitMatrixToBitmap(bitMatrix)
    }

    /**
     * Generate a Data Matrix 2D barcode from an arbitrary string.
     * Used for combined/concatenated data in batch tickets.
     */
    fun generateDataMatrix(
        data: String,
        size: Int = 200
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 0,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val writer = DataMatrixWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.DATA_MATRIX, size, size, hints)
        return bitMatrixToBitmap(bitMatrix)
    }

    /**
     * Format expiry date as DD-MM-YYYY for display.
     */
    fun formatExpiryDate(expiryDateMillis: Long): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return dateFormat.format(Date(expiryDateMillis))
    }

    /**
     * Format barcode with GS1 AI (97) prefix for display.
     */
    fun formatBarcodeWithAI(barcode: String): String {
        return "(97)$barcode"
    }

    private fun bitMatrixToBitmap(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
