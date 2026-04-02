package com.example.datewisepos.util

import android.graphics.*

/**
 * Generates a ticket bitmap with side-by-side layout:
 *
 * ┌──────────────────────────────────────┐
 * │  Expires DD-MM-YYYY       EXPDT  ✅  │
 * │                                      │
 * │  ║║║║║║║║║║║║║║║║║║║║║║  ┌────────┐  │
 * │  ║║║║║║║║║║║║║║║║║║║║║║  │▓▓▓▓▓▓▓▓│  │
 * │  ║║║║║║║║║║║║║║║║║║║║║║  │▓▓▓▓▓▓▓▓│  │
 * │  ║║║║║║║║║║║║║║║║║║║║║║  └────────┘  │
 * │                                      │
 * │  (97)XXXXXXXXXXXXXXXX     EXPDT ↑   │
 * └──────────────────────────────────────┘
 */
object TicketGenerator {

    private const val TICKET_WIDTH = 384  // Standard 58mm thermal at 203 DPI

    fun generateTicket(
        barcode: String,
        expiryDateMillis: Long,
        productName: String = "",
        productBrand: String = "",
        productQuantity: String = ""
    ): Bitmap {
        val expiryDateStr = BarcodeGenerator.formatExpiryDate(expiryDateMillis)
        val barcodeWithAI = BarcodeGenerator.formatBarcodeWithAI(barcode)

        val dmSize = 80
        val padding = 16
        val spacingBetweenBarcodeAndDm = 12
        val barcodeWidth = TICKET_WIDTH - (padding * 2) - dmSize - spacingBetweenBarcodeAndDm
        val barcodeHeight = 120

        // Generate barcode images
        val barcodeImage = BarcodeGenerator.generateBarcode(barcode, width = barcodeWidth, height = barcodeHeight)
        val dataMatrixImage = BarcodeGenerator.generateExpiryDataMatrix(
            expiryDateMillis, barcode = barcode, name = productName, brand = productBrand,
            quantity = productQuantity, size = dmSize
        )

        // Calculate ticket dimensions
        val headerHeight = 40
        val barcodeRowHeight = maxOf(barcodeHeight, dmSize)
        val footerHeight = 40
        val spacingBetweenRows = 12
        val bottomPadding = 80
        val totalHeight = padding + headerHeight + spacingBetweenRows +
                barcodeRowHeight + spacingBetweenRows + footerHeight + bottomPadding

        // Create bitmap
        val bitmap = Bitmap.createBitmap(TICKET_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // === HEADER ROW ===
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val expiresText = "Expires $expiryDateStr"
        canvas.drawText(expiresText, padding.toFloat(), (padding + 26).toFloat(), headerPaint)

        // "EXPDT ✅" badge
        val badgePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val badgeText = "EXPDT"
        val badgeTextWidth = badgePaint.measureText(badgeText)

        val checkboxSize = 22f
        val badgeX = TICKET_WIDTH - padding - badgeTextWidth - 6 - checkboxSize
        val badgeY = padding + 22f
        canvas.drawText(badgeText, badgeX, badgeY, badgePaint)

        // Green checkbox
        val checkPaint = Paint().apply {
            color = Color.rgb(76, 175, 80)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val checkRect = RectF(
            badgeX + badgeTextWidth + 6,
            badgeY - 16,
            badgeX + badgeTextWidth + 6 + checkboxSize,
            badgeY + 6
        )
        canvas.drawRoundRect(checkRect, 4f, 4f, checkPaint)

        // Checkmark
        val checkmarkPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        val cx = checkRect.centerX()
        val cy = checkRect.centerY()
        val path = Path()
        path.moveTo(cx - 5, cy)
        path.lineTo(cx - 2, cy + 4)
        path.lineTo(cx + 6, cy - 4)
        canvas.drawPath(path, checkmarkPaint)

        // === BARCODE ROW ===
        val barcodeRowY = padding + headerHeight + spacingBetweenRows

        // Product barcode (left)
        val barcodeLeft = padding
        val barcodeTop = barcodeRowY + (barcodeRowHeight - barcodeImage.height) / 2
        canvas.drawBitmap(barcodeImage, barcodeLeft.toFloat(), barcodeTop.toFloat(), null)

        // DataMatrix (right)
        val dmLeft = TICKET_WIDTH - padding - dataMatrixImage.width
        val dmTop = barcodeRowY + (barcodeRowHeight - dataMatrixImage.height) / 2
        canvas.drawBitmap(dataMatrixImage, dmLeft.toFloat(), dmTop.toFloat(), null)

        // === FOOTER ROW ===
        val footerY = barcodeRowY + barcodeRowHeight + spacingBetweenRows

        // Barcode number "(97)XXXXXXXX"
        val footerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText(barcodeWithAI, padding.toFloat(), (footerY + 16).toFloat(), footerPaint)

        // "EXPDT ↑"
        val dmLabelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val dmLabelText = "EXPDT ↑"
        val dmLabelWidth = dmLabelPaint.measureText(dmLabelText)
        val dmLabelX = dmLeft + (dataMatrixImage.width - dmLabelWidth) / 2
        canvas.drawText(dmLabelText, dmLabelX, (footerY + 16).toFloat(), dmLabelPaint)

        // Clean up
        barcodeImage.recycle()
        dataMatrixImage.recycle()

        return bitmap
    }
}
