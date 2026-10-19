package com.video.app

import android.content.Context
import org.json.JSONArray
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Todas las preferencias de la app. Los valores por defecto están pensados para este tipo de celular (MediaTek, poca RAM). */
class Ajustes(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("video", Context.MODE_PRIVATE)

    private inner class Ent(val k: String, val d: Int) : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = p.getInt(k, d)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) = p.edit().putInt(k, value).apply()
    }

    private inner class Bool(val k: String, val d: Boolean) : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = p.getBoolean(k, d)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) = p.edit().putBoolean(k, value).apply()
    }

    var carpetas: List<String>
        get() = p.getString("carpetas", null)?.let { s -> JSONArray(s).let { a -> List(a.length()) { a.getString(it) } } } ?: emptyList()
        set(v) = p.edit().putString("carpetas", JSONArray(v).toString()).apply()

    // Decodificador
    var politica by Ent("politica", POL_HW_PRIMERO)
    var colaMediaCodec by Ent("cola", COLA_AUTO)
    var fallback by Bool("fallback", true)
    var tunel by Bool("tunel", false)
    var buffer by Ent("buffer", BUF_EQUILIBRADO)

    // Reproducción
    var segundoPlano by Bool("segundo_plano", true)
    var ofloadAudio by Bool("offload_audio", true)
    var saltarSilencios by Bool("saltar_silencios", false)
    var velocidad by Ent("velocidad_x100", 100)
    var diagnostico by Bool("diagnostico", false)
    var rotacion by Ent("rotacion", 0)

    // Biblioteca y aspecto
    var tema by Ent("tema", 0)
    var orden by Ent("orden", ORDEN_FECHA)
    var profundidad by Ent("profundidad", 0)
    var ocultas by Bool("ocultas", false)

    fun decoderFijado(mime: String): String? = p.getString("fijado_$mime", null)
    fun fijarDecoder(mime: String, nombre: String?) {
        val e = p.edit()
        if (nombre == null) e.remove("fijado_$mime") else e.putString("fijado_$mime", nombre)
        e.apply()
    }

    companion object {
        const val POL_SISTEMA = 0
        const val POL_HW_PRIMERO = 1
        const val POL_SOLO_HW = 2
        const val POL_SW_PRIMERO = 3
        val POLITICAS = listOf("Automático del sistema", "Hardware primero (recomendado)", "Solo hardware", "Software primero")

        const val COLA_AUTO = 0
        val COLAS = listOf("Automática", "Asíncrona (más fluida)", "Síncrona (más compatible)")

        const val BUF_AHORRO = 0
        const val BUF_EQUILIBRADO = 1
        const val BUF_FLUIDO = 2
        val BUFFERS = listOf("Ahorro (poca RAM)", "Equilibrado", "Fluido (más RAM)")
        // mínimo, máximo, para empezar, tras congelarse (ms)
        val BUFFER_MS = listOf(intArrayOf(2000, 8000, 1000, 2000), intArrayOf(5000, 20000, 1500, 3000), intArrayOf(10000, 50000, 2500, 5000))

        const val ORDEN_FECHA = 0
        val ORDENES = listOf("Más recientes", "Nombre", "Tamaño")
        val PROFUNDIDADES = listOf("Sin límite" to 0, "1 nivel" to 1, "2 niveles" to 2, "4 niveles" to 4)
        val VELOCIDADES = listOf("0.75x" to 75, "1x" to 100, "1.25x" to 125, "1.5x" to 150, "2x" to 200)
    }
}
