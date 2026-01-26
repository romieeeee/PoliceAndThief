package com.d104.pnt.ui.game.play.mission

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * 이미지 메타데이터 정보
 */
data class ImageInfo(
    val width: Int,
    val height: Int,
    val fileSizeKB: Long,
    val quality: Int
)

/**
 * EXIF 정보를 읽어서 이미지 회전 각도를 반환
 */
private fun getImageRotation(file: File): Int {
    return try {
        val exif = ExifInterface(file.absolutePath)
        when (exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        Timber.e("EXIF 읽기 실패: ${e.message}")
        0
    }
}

/**
 * 비트맵을 회전시키는 함수
 */
private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap

    val matrix = Matrix().apply {
        postRotate(degrees.toFloat())
    }

    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        matrix,
        true
    )
}

/**
 * 이미지 파일을 압축하는 함수
 * @param originalFile 원본 이미지 파일
 * @param maxWidth 최대 가로 해상도 (기본 1280px)
 * @param maxHeight 최대 세로 해상도 (기본 720px)
 * @param quality 압축 품질 (0-100, 기본 80)
 * @return 압축된 이미지 파일
 */
fun compressImage(
    originalFile: File,
    maxWidth: Int = 1280,
    maxHeight: Int = 720,
    quality: Int = 80
): File {
    // EXIF에서 회전 정보 읽기
    val rotation = getImageRotation(originalFile)
    Timber.d("이미지 회전 각도: $rotation")

    // 원본 이미지 로드
    var originalBitmap = BitmapFactory.decodeFile(originalFile.absolutePath)

    // 회전 적용
    if (rotation != 0) {
        val rotatedBitmap = rotateBitmap(originalBitmap, rotation)
        originalBitmap.recycle()
        originalBitmap = rotatedBitmap
    }

    // 원본 크기
    val originalWidth = originalBitmap.width
    val originalHeight = originalBitmap.height

    Timber.d("회전 적용 후 크기: ${originalWidth}x${originalHeight}")

    // 비율 유지하면서 리사이징
    val scale = minOf(
        maxWidth.toFloat() / originalWidth,
        maxHeight.toFloat() / originalHeight,
        1.0f // 원본보다 크게 만들지 않음
    )

    val newWidth = (originalWidth * scale).toInt()
    val newHeight = (originalHeight * scale).toInt()

    Timber.d("압축 후 크기: ${newWidth}x${newHeight}, 품질: $quality")

    // 리사이징
    val resizedBitmap = Bitmap.createScaledBitmap(
        originalBitmap,
        newWidth,
        newHeight,
        true // 고품질 필터링
    )

    // 압축된 파일 저장
    val compressedFile = File(
        originalFile.parent,
        "compressed_${originalFile.name}"
    )

    FileOutputStream(compressedFile).use { out ->
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    }

    // EXIF 정보 복사 (회전 정보는 제거 - 이미 회전 적용했으므로)
    try {
        val srcExif = ExifInterface(originalFile.absolutePath)
        val destExif = ExifInterface(compressedFile.absolutePath)

        // 필요한 EXIF 정보만 복사 (위치 정보 등)
        destExif.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL.toString()
        )
        destExif.saveAttributes()
    } catch (e: Exception) {
        Timber.e("EXIF 복사 실패: ${e.message}")
    }

    // 메모리 해제
    originalBitmap.recycle()
    resizedBitmap.recycle()

    Timber.d("압축 완료: ${compressedFile.length() / 1024}KB")

    return compressedFile
}

/**
 * 이미지 파일의 메타데이터를 가져오는 함수
 */
fun getImageInfo(file: File, quality: Int = 80): ImageInfo {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true // 메타데이터만 읽기 (메모리 절약)
    }
    BitmapFactory.decodeFile(file.absolutePath, options)

    return ImageInfo(
        width = options.outWidth,
        height = options.outHeight,
        fileSizeKB = file.length() / 1024,
        quality = quality
    )
}