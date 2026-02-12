package com.mealsai.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {
    
    /**
     * Convert Bitmap to Base64 encoded string
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress before converting to base64
        val compressedBitmap = compressImage(bitmap, maxSizeKB = 500)
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    }
    
    /**
     * Convert URI to Base64 encoded string
     */
    fun uriToBase64(uri: Uri, context: Context): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.let { bitmapToBase64(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Compress bitmap to reduce file size
     * @param bitmap Original bitmap
     * @param maxSizeKB Maximum size in KB
     * @return Compressed bitmap
     */
    fun compressImage(bitmap: Bitmap, maxSizeKB: Int = 500): Bitmap {
        var quality = 85
        var compressedBitmap = bitmap
        
        // Calculate current size
        val outputStream = ByteArrayOutputStream()
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        var sizeKB = outputStream.size() / 1024
        
        // Reduce quality until size is acceptable
        while (sizeKB > maxSizeKB && quality > 20) {
            quality -= 10
            outputStream.reset()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            sizeKB = outputStream.size() / 1024
        }
        
        // If still too large, resize the bitmap
        if (sizeKB > maxSizeKB) {
            var scale = 0.9f
            while (sizeKB > maxSizeKB && scale > 0.3f) {
                val width = (bitmap.width * scale).toInt()
                val height = (bitmap.height * scale).toInt()
                compressedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
                
                outputStream.reset()
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                sizeKB = outputStream.size() / 1024
                scale -= 0.1f
            }
        }
        
        return compressedBitmap
    }
    
    /**
     * Get bitmap from URI
     */
    fun getBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
