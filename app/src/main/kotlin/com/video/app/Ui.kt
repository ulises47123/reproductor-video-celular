package com.video.app

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/** Piezas de interfaz comunes: mismo estilo oscuro con rojo y verde del tema. */
object Ui {
    // Colores del tema activo. ROJO = acento principal, VERDE = acento secundario (los nombres vienen del primer tema).
    var FONDO = Color.parseColor("#0E1114")
    var ROJO = Color.parseColor("#E0455A")
    var VERDE = Color.parseColor("#B8D86B")
    var TENUE = Color.parseColor("#8FA85A")
    var TEXTO = Color.parseColor("#E6E6E6")
    var SUPERFICIE = Color.parseColor("#1A2024")

    val TEMAS = listOf("Rojo y verde", "Morado apagado", "Oscuro")

    /** Cada pantalla lo llama antes de armar su interfaz. */
    fun aplicar(ctx: Context) {
        val c = when (Ajustes(ctx).tema) {
            1 -> arrayOf("#17141C", "#9A7FB8", "#C4B5D6", "#7C6E8F", "#E4DFEA", "#231E2B")
            2 -> arrayOf("#0B0B0B", "#E0E0E0", "#A8A8A8", "#767676", "#EDEDED", "#1A1A1A")
            else -> arrayOf("#0E1114", "#E0455A", "#B8D86B", "#8FA85A", "#E6E6E6", "#1A2024")
        }.map { Color.parseColor(it) }
        FONDO = c[0]; ROJO = c[1]; VERDE = c[2]; TENUE = c[3]; TEXTO = c[4]; SUPERFICIE = c[5]
    }

    fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()

    /** Deja espacio para las barras del sistema (Android 15 dibuja la app detrás de ellas). */
    fun insets(v: View) {
        v.setOnApplyWindowInsetsListener { vista, ins ->
            val b = ins.getInsets(WindowInsets.Type.systemBars())
            vista.setPadding(b.left, b.top, b.right, b.bottom)
            ins
        }
    }

    fun texto(ctx: Context, t: String, tam: Float = 15f, color: Int = VERDE, negrita: Boolean = false) = TextView(ctx).apply {
        text = t
        textSize = tam
        setTextColor(color)
        typeface = Typeface.create(Typeface.MONOSPACE, if (negrita) Typeface.BOLD else Typeface.NORMAL)
    }

    fun titulo(ctx: Context, t: String) = texto(ctx, t, 13f, ROJO, true).apply {
        setPadding(dp(ctx, 16), dp(ctx, 20), dp(ctx, 16), dp(ctx, 6))
    }

    fun boton(ctx: Context, t: String, accion: () -> Unit) = texto(ctx, t, 14f, ROJO, true).apply {
        setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
        gravity = Gravity.CENTER
        setOnClickListener { accion() }
    }

    /** Fila de ajuste: título, valor actual debajo y acción al tocar. */
    fun fila(ctx: Context, titulo: String, valor: String, accion: () -> Unit) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(ctx, 16), dp(ctx, 12), dp(ctx, 16), dp(ctx, 12))
        addView(texto(ctx, titulo, 15f, TEXTO))
        addView(texto(ctx, valor, 12f, TENUE).apply { tag = "valor" })
        setOnClickListener { accion() }
    }

    fun interruptor(ctx: Context, titulo: String, detalle: String, activo: Boolean, cambio: (Boolean) -> Unit) = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(ctx, 16), dp(ctx, 12), dp(ctx, 16), dp(ctx, 12))
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(texto(ctx, titulo, 15f, TEXTO))
            addView(texto(ctx, detalle, 12f, TENUE))
        }
        addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        val sw = Switch(ctx).apply {
            isChecked = activo
            setOnCheckedChangeListener { _, v -> cambio(v) }
        }
        addView(sw)
        setOnClickListener { sw.toggle() }
    }

    /** Diálogo de selección única; devuelve el índice elegido. */
    fun elegir(ctx: Context, titulo: String, opciones: List<String>, actual: Int, elegido: (Int) -> Unit) {
        AlertDialog.Builder(ctx, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(titulo)
            .setSingleChoiceItems(opciones.toTypedArray(), actual) { d, i -> elegido(i); d.dismiss() }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
