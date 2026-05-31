package com.movtery.zalithlauncher.utils.skin

import android.graphics.BitmapFactory
import java.io.File

object SkinFileValidator {
    fun isValidSkinFile(file: File): Boolean {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth == 64 && (options.outHeight == 64 || options.outHeight == 32)
        } catch (e: Exception) {
            false
        }
    }

    fun isValidCapeFile(file: File): Boolean {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.outWidth == 64 && (options.outHeight == 32 || options.outHeight == 64)
        } catch (e: Exception) {
            false
        }
    }
}
