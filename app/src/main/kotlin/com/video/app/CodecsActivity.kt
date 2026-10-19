package com.video.app

import android.app.Activity
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView

/** Lista los decodificadores de video que tiene este celular y deja fijar uno por códec. */
class CodecsActivity : Activity() {
    private lateinit var aj: Ajustes
    private lateinit var cuerpo: LinearLayout

    private val mimes = listOf(
        "video/avc" to "H.264 (AVC)",
        "video/hevc" to "H.265 (HEVC)",
        "video/x-vnd.on2.vp9" to "VP9",
        "video/av01" to "AV1",
        "video/mp4v-es" to "MPEG-4",
        "video/x-vnd.on2.vp8" to "VP8",
        "video/3gpp" to "H.263",
    )

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        aj = Ajustes(this)
        Ui.aplicar(this)
        cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.FONDO)
            addView(Ui.texto(context, "Decodificadores", 22f, Ui.ROJO, true).apply { setPadding(Ui.dp(context, 16), Ui.dp(context, 12), 0, Ui.dp(context, 4)) })
            addView(
                Ui.texto(context, "HW = usa el chip de video (rápido, poca batería). SW = lo hace el procesador (lento, caliente). Toca uno para fijarlo; «Automático» sigue la política de Ajustes.", 12f, Ui.TENUE).apply {
                    setPadding(Ui.dp(context, 16), 0, Ui.dp(context, 16), Ui.dp(context, 8))
                },
            )
            addView(ScrollView(context).apply { addView(cuerpo) }, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        Ui.insets(raiz)
        setContentView(raiz)
        pintar()
    }

    private fun etiqueta(i: MediaCodecInfo): String {
        val tipo = if (i.isHardwareAccelerated && !i.isSoftwareOnly) "HW" else "SW"
        return if (i.isVendor) "$tipo · fabricante" else "$tipo · Google"
    }

    private fun limites(i: MediaCodecInfo, mime: String): String = try {
        val v = i.getCapabilitiesForType(mime).videoCapabilities
        if (v == null) "" else "hasta ${v.supportedWidths.upper}x${v.supportedHeights.upper}"
    } catch (e: Exception) {
        ""
    }

    private fun pintar() {
        cuerpo.removeAllViews()
        val c = this
        val todos = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.filter { !it.isEncoder }
        for ((mime, nombre) in mimes) {
            val decs = todos.filter { d -> d.supportedTypes.any { it.equals(mime, true) } }
            if (decs.isEmpty()) continue
            val fijado = aj.decoderFijado(mime)
            cuerpo.addView(Ui.titulo(c, nombre.uppercase()))
            cuerpo.addView(Ui.fila(c, (if (fijado == null) "● " else "○ ") + "Automático", "según la política elegida") {
                aj.fijarDecoder(mime, null)
                pintar()
            })
            for (d in decs) {
                val activo = d.name == fijado
                cuerpo.addView(Ui.fila(c, (if (activo) "● " else "○ ") + d.name, "${etiqueta(d)} · ${limites(d, mime)}") {
                    aj.fijarDecoder(mime, d.name)
                    pintar()
                })
            }
        }
    }
}
