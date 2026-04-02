package com.example.datewisepos.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.sunmi.printerx.PrinterSdk
import com.sunmi.printerx.PrinterSdk.PrinterListen
import com.sunmi.printerx.PrinterSdk.Printer
import com.sunmi.printerx.api.LineApi
import com.sunmi.printerx.api.PrintResult
import com.sunmi.printerx.style.BaseStyle
import com.sunmi.printerx.style.BitmapStyle
import com.sunmi.printerx.enums.Align
import com.sunmi.printerx.enums.ImageAlgorithm
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Helper to interact with the Sunmi V2 Pro built-in thermal printer
 * using the modern Sunmi PrinterX official SDK.
 */
class SunmiPrinterHelper(private val context: Context) {

    private var sunmiPrinter: Printer? = null

    companion object {
        private const val TAG = "SunmiPrinter"
    }

    private val printerListen = object : PrinterListen {
        override fun onDefPrinter(printer: Printer?) {
            sunmiPrinter = printer
            Log.i(TAG, "Sunmi printerx onDefPrinter connected")
        }

        override fun onPrinters(printers: MutableList<Printer>?) {
            if (sunmiPrinter == null && printers?.isNotEmpty() == true) {
                sunmiPrinter = printers.first()
                Log.i(TAG, "Sunmi printerx onPrinters connected")
            }
        }
    }

    init {
        bindService()
    }

    private fun bindService() {
        try {
            PrinterSdk.getInstance().getPrinter(context, printerListen)
        } catch (e: Exception) {
            Log.w(TAG, "Sunmi printerx service not available: ${e.message}")
        }
    }

    /**
     * Ensure the printer is connected, rebinding if needed.
     */
    private suspend fun ensurePrinterConnected() {
        if (sunmiPrinter == null) {
            Log.w(TAG, "Printer not connected, attempting to rebind...")
            bindService()

            var retries = 0
            while (sunmiPrinter == null && retries < 10) {
                kotlinx.coroutines.delay(200)
                retries++
            }

            if (sunmiPrinter == null) {
                throw Exception("Sunmi printer service unavailable. Make sure this is a Sunmi device.")
            }
        }
    }

    /**
     * Force a fresh reconnection to the printer service.
     */
    private suspend fun reconnect() {
        sunmiPrinter = null
        try {
            PrinterSdk.getInstance().destroy()
        } catch (_: Exception) {}
        kotlinx.coroutines.delay(300)
        bindService()

        var retries = 0
        while (sunmiPrinter == null && retries < 15) {
            kotlinx.coroutines.delay(200)
            retries++
        }
    }

    /**
     * Actually send the bitmap to the printer.
     */
    private suspend fun doPrint(bitmap: Bitmap) {
        val printer = sunmiPrinter
            ?: throw Exception("Printer not connected")
        val lineApi = printer.lineApi()

        // Enable transaction mode for reliable printing
        lineApi.enableTransMode(true)

        // Setup alignment
        val baseStyle = BaseStyle.getStyle().setAlign(Align.CENTER)
        lineApi.initLine(baseStyle)

        // Print bitmap with Binarization algorithm
        val bitmapStyle = BitmapStyle.getStyle().setAlgorithm(ImageAlgorithm.BINARIZATION)
        lineApi.printBitmap(bitmap, bitmapStyle)

        // Automatically feed paper to cut/exit position
        lineApi.autoOut()

        // Finalize printing in a coroutine
        suspendCancellableCoroutine<Unit> { continuation ->
            lineApi.printTrans(object : PrintResult() {
                override fun onResult(resultCode: Int, message: String?) {
                    Log.d(TAG, "printTrans result: code=$resultCode, msg=$message")
                    if (continuation.isActive) {
                        if (resultCode == 0) {
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(
                                Exception("Print failed: $message (code $resultCode)")
                            )
                        }
                    }
                }
            })
        }

        Log.i(TAG, "Ticket printed successfully")
    }

    /**
     * Print a bitmap on the Sunmi built-in printer.
     * Automatically reconnects if the service has disconnected.
     */
    suspend fun printBitmap(bitmap: Bitmap) {
        ensurePrinterConnected()

        try {
            doPrint(bitmap)
        } catch (e: Exception) {
            // Service may have silently disconnected — try reconnecting once
            Log.w(TAG, "First print attempt failed (${e.message}), reconnecting...")
            reconnect()

            if (sunmiPrinter == null) {
                throw Exception("Print service disconnected and could not reconnect.")
            }

            try {
                doPrint(bitmap)
            } catch (e2: Exception) {
                Log.e(TAG, "Print failed after reconnect", e2)
                throw Exception("Print failed: ${e2.message}")
            }
        }
    }

    fun disconnect() {
        try {
            PrinterSdk.getInstance().destroy()
            sunmiPrinter = null
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying printer SDK", e)
        }
    }
}
