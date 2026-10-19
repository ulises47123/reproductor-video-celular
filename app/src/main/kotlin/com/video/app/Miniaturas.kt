package com.video.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.util.LruCache
import android.widget.ImageView
import java.util.concurrent.Executors

/** Miniaturas en segundo plano con caché en memoria acotada; el proveedor primero, el primer cuadro del video si no hay. */
object Miniaturas {
    // Acotado por bytes (6 MB), no por cantidad: así la memoria no crece con la lista.
    private val cache = object : LruCache<String, Bitmap>(6 * 1024 * 1024) {
        override fun sizeOf(clave: String, valor: Bitmap) = valor.byteCount
    }
    private val pool = Executors.newFixedThreadPool(2)
    private val ui = Handler(Looper.getMainLooper())

    fun cargar(ctx: Context, v: Video, destino: ImageView) {
        destino.tag = v.uriTxt
        val ya = cache.get(v.uriTxt)
        if (ya != null) {
            destino.setImageBitmap(ya)
            return
        }
        destino.setImageDrawable(null)
        val app = ctx.applicationContext
        pool.execute {
            val bmp = delProveedor(app, v) ?: primerCuadro(app, v)
            if (bmp != null) {
                cache.put(v.uriTxt, bmp)
                ui.post { if (destino.tag == v.uriTxt) destino.setImageBitmap(bmp) }
            }
        }
    }

    private fun delProveedor(ctx: Context, v: Video): Bitmap? = try {
        DocumentsContract.getDocumentThumbnail(ctx.contentResolver, v.uri, Point(320, 240), null)
    } catch (e: Exception) {
        null
    }

    private fun primerCuadro(ctx: Context, v: Video): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, v.uri)
            r.getScaledFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 320, 240)
        } catch (e: Exception) {
            null
        } finally {
            try { r.release() } catch (e: Exception) { }
        }
    }
}
