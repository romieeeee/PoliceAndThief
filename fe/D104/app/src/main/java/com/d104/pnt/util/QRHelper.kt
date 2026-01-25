package com.d104.pnt.util

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

fun generateQrCode(content: String, size: Int = 256): Bitmap? {
    return try {
        val barcodeEncoder = BarcodeEncoder()
        // 핵심: 복잡한 픽셀 변환 로직 없이 바로 비트맵을 뱉어줍니다.
        barcodeEncoder.encodeBitmap(content, BarcodeFormat.QR_CODE, size, size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}