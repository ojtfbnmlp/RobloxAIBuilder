package com.robloxbuilder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import java.io.ByteArrayOutputStream

object ImageHolder {
    var listener: ((String) -> Unit)? = null

    fun encode(ctx: Context, uri: Uri): String? {
        try {
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            val s1 = ctx.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(s1, null, bounds)
            s1?.close()
            var sample = 1
            while (bounds.outWidth / sample > 2000 || bounds.outHeight / sample > 2000) {
                sample *= 2
            }
            val opts = BitmapFactory.Options()
            opts.inSampleSize = sample
            val s2 = ctx.contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(s2, null, opts)
            s2?.close()
            if (bmp == null) return null
            val big = maxOf(bmp.width, bmp.height)
            val scale = 1024f / big
            val out = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bmp,
                    (bmp.width * scale).toInt(),
                    (bmp.height * scale).toInt(),
                    true
                )
            } else {
                bmp
            }
            val bos = ByteArrayOutputStream()
            out.compress(Bitmap.CompressFormat.JPEG, 80, bos)
            return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            return null
        }
    }

    fun thumb(ctx: Context, b64: String): ImageView {
        val bytes = Base64.decode(b64, Base64.NO_WRAP)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val iv = ImageView(ctx)
        iv.setImageBitmap(bmp)
        iv.adjustViewBounds = true
        iv.maxHeight = Views.dp(ctx, 140)
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        val lp = LinearLayout.LayoutParams(-2, -2)
        lp.topMargin = Views.dp(ctx, 6)
        lp.gravity = Gravity.END
        iv.layoutParams = lp
        return iv
    }
}
