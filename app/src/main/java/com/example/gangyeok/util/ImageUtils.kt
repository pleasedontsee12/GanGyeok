// util/ImageUtils.kt
package com.example.gangyeok.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

fun compressImageToBase64(file: File): String {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    val aspectRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
    val width = 500
    val height = (width / aspectRatio).toInt()
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
    val byteArray = outputStream.toByteArray()

    return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT)
}