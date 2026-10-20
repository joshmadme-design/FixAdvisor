package com.shahab.fixadvisor.data.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.security.MessageDigest
import kotlin.math.abs

/**
 * QR helpers for the shop counter sticker (see ShopQrScreen).
 *
 * The QR encodes [reviewPageUrl]: a placeholder web route for the shop's
 * review page (`https://fixadvisor.app/r/{shopId}`). There is no website yet —
 * Phase 1 treats the URL as an identifier; a real site or App Links can adopt
 * the same route later without reprinting stickers.
 */
object QrCodeUtils {

    fun reviewPageUrl(shopId: String): String = "https://fixadvisor.app/r/$shopId"

    /**
     * Renders a QR code bitmap for [content] at [sizePx] pixels.
     * Returns null only if the ZXing encoder rejects the content.
     */
    fun makeQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Stable 6-digit verification code for a shop, derived from the shop id.
     * The owner reads it to customers after a repair; a review submitted with
     * the matching code is marked "verified". Deterministic so the owner
     * always sees the same code — no code storage or sync needed.
     */
    fun verificationCodeFor(shopId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(shopId.toByteArray(Charsets.UTF_8))
        var value = 0
        for (i in 0 until 4) {
            value = (value shl 8) or (digest[i].toInt() and 0xFF)
        }
        val code = abs(value) % 900_000 + 100_000
        return code.toString()
    }
}
