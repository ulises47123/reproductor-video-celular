package com.video.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

/** Aquí se decide dónde mira la app. Solo se leen las carpetas de esta lista. */
class CarpetasActivity : Activity() {
    private lateinit var aj: Ajustes
    private lateinit var lista: LinearLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        aj = Ajustes(this)
        Ui.aplicar(this)
        lista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.FONDO)
            addView(Ui.texto(context, "Carpetas", 22f, Ui.ROJO, true).apply { setPadding(Ui.dp(context, 16), Ui.dp(context, 12), 0, Ui.dp(context, 4)) })
            addView(
                Ui.texto(context, "La app solo mira estas carpetas y sus subcarpetas. No recorre el resto del celular. Se pide también permiso de escritura, necesario para poder borrar videos desde la galería.", 12f, Ui.TENUE).apply {
                    setPadding(Ui.dp(context, 16), 0, Ui.dp(context, 16), Ui.dp(context, 8))
                },
            )
            addView(LinearLayout(context).apply {
                addView(Ui.boton(context, "+ Agregar carpeta") { elegir(null) })
                addView(Ui.boton(context, "+ Termux") { elegir(TERMUX) })
            })
            addView(ScrollView(context).apply { addView(lista) }, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        Ui.insets(raiz)
        setContentView(raiz)
        pintar()
    }

    private fun elegir(inicial: Uri?) {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
        )
        if (inicial != null) i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, inicial)
        try {
            startActivityForResult(i, 10)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el selector de carpetas", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(codigo: Int, resultado: Int, datos: Intent?) {
        super.onActivityResult(codigo, resultado, datos)
        val uri = datos?.data ?: return
        if (codigo != 10 || resultado != RESULT_OK) return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } catch (e: Exception) {
            // Algunos proveedores (p. ej. de solo lectura) no dan escritura: se guarda al menos la lectura.
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Toast.makeText(this, "Esta carpeta es de solo lectura: se podrá ver pero no borrar", Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Toast.makeText(this, "Android no permitió guardar el acceso a esta carpeta", Toast.LENGTH_LONG).show()
                return
            }
        }
        if (uri.toString() !in aj.carpetas) aj.carpetas = aj.carpetas + uri.toString()
        pintar()
    }

    private fun pintar() {
        lista.removeAllViews()
        val ctx = this
        if (aj.carpetas.isEmpty()) {
            lista.addView(Ui.texto(ctx, "No hay carpetas elegidas.", 14f).apply { setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 24), 0, 0) })
            return
        }
        for (txt in aj.carpetas) {
            val uri = Uri.parse(txt)
            val id = try { DocumentsContract.getTreeDocumentId(uri) } catch (e: Exception) { txt }
            val fila = LinearLayout(ctx).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 10), Ui.dp(ctx, 8), Ui.dp(ctx, 10))
                addView(
                    LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(Ui.texto(ctx, id.substringAfterLast('/').substringAfterLast(':').ifBlank { "Carpeta" }, 15f, Ui.TEXTO))
                        addView(Ui.texto(ctx, id, 10f, Ui.TENUE))
                    },
                    LinearLayout.LayoutParams(0, -2, 1f),
                )
                addView(Ui.boton(ctx, "Quitar") {
                    try { contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (e: Exception) { }
                    aj.carpetas = aj.carpetas - txt
                    pintar()
                })
            }
            lista.addView(fila)
        }
    }

    companion object {
        // Termux publica su carpeta de trabajo como proveedor de documentos; aquí solo se sugiere como punto de partida.
        val TERMUX: Uri = DocumentsContract.buildDocumentUri("com.termux.documents", "/data/data/com.termux/files/home")
    }
}
