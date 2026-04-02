package com.example.datewisepos.util

import android.graphics.*
import com.example.datewisepos.ui.batchticket.ScannedTicketItem

/**
 * Generates a combined batch ticket bitmap with a single large DataMatrix
 * encoding all product info.
 *
 * Data format: "name1|brand1|barcode1|quantity1|expiry1;name2|brand2|barcode2|quantity2|expiry2;..."
 *
 * ┌──────────────────────────────────────┐
 * │  BATCH TICKET — N items              │
 * │                                      │
 * │         ┌──────────────┐             │
 * │         │▓▓▓▓▓▓▓▓▓▓▓▓▓▓│             │
 * │         │▓▓▓▓▓▓▓▓▓▓▓▓▓▓│             │
 * │         │▓▓▓▓▓▓▓▓▓▓▓▓▓▓│             │
 * │         │▓▓▓▓▓▓▓▓▓▓▓▓▓▓│             │
 * │         └──────────────┘             │
 * │          SCAN TO READ ↑              │
 * └──────────────────────────────────────┘
 */
object BatchTicketGenerator {

    private const val TICKET_WIDTH = 384  // 58mm thermal at 203 DPI

    fun generateBatchTicket(items: List<ScannedTicketItem>): Bitmap {
        // Combine all data: "name1|brand1|barcode1|quantity1|expiry1;name2|brand2|barcode2|quantity2|expiry2;..."
        val combinedData = items.joinToString(";") {
            "${it.name}|${it.brand}|${it.barcode}|${it.quantity}|${it.expiryDate}"
        }

        val padding = 16

        // Scale the DataMatrix size based on data length for readability
        // More items = more data = needs a larger matrix
        val dataLength = combinedData.length
        val dmSize = when {
            dataLength <= 50 -> 250
            dataLength <= 100 -> 280
            dataLength <= 200 -> 320
            else -> TICKET_WIDTH - (padding * 2) // Max: fill the ticket width
        }.coerceAtMost(TICKET_WIDTH - (padding * 2))

        // Generate the single combined DataMatrix
        val dataMatrixImage = BarcodeGenerator.generateDataMatrix(combinedData, size = dmSize)

        // Layout measurements
        val headerHeight = 40
        val spacingAfterHeader = 12
        val spacingAfterDm = 8
        val footerLabelHeight = 24
        val bottomPadding = 80

        val totalHeight = padding + headerHeight + spacingAfterHeader +
                dmSize + spacingAfterDm + footerLabelHeight + bottomPadding

        val bitmap = Bitmap.createBitmap(TICKET_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // === HEADER ===
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val headerText = "BATCH TICKET — ${items.size} item${if (items.size != 1) "s" else ""}"
        canvas.drawText(headerText, padding.toFloat(), (padding + 26).toFloat(), headerPaint)

        // === DATAMATRIX (centered) ===
        val dmY = padding + headerHeight + spacingAfterHeader
        val dmX = (TICKET_WIDTH - dataMatrixImage.width) / 2
        canvas.drawBitmap(dataMatrixImage, dmX.toFloat(), dmY.toFloat(), null)

        // === FOOTER LABEL ===
        val footerY = dmY + dmSize + spacingAfterDm
        val footerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val labelText = "SCAN TO READ ↑"
        val labelWidth = footerPaint.measureText(labelText)
        val labelX = (TICKET_WIDTH - labelWidth) / 2
        canvas.drawText(labelText, labelX, (footerY + 16).toFloat(), footerPaint)

        // Clean up
        dataMatrixImage.recycle()

        return bitmap
    }
}
