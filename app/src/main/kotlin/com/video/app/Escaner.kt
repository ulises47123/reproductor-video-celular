package com.video.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class Video(val uri: Uri, val nombre: String, val tam: Long, val fecha: Long, val carpeta: String) {
    val uriTxt: String get() = uri.toString()
}

/**
 * Recorre SOLO las carpetas que el usuario eligió (permiso de árbol de Android), nunca todo el celular.
 * Funciona igual con carpetas normales y con proveedores como el de Termux.
 */
object Escaner {
    private val EXTENSIONES = setOf("mp4", "mkv", "webm", "mov", "avi", "3gp", "m4v", "ts", "flv", "wmv", "mpg", "mpeg")
    private val COLUMNAS = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )

    fun escanear(ctx: Context, aj: Ajustes): List<Video> {
        val salida = ArrayList<Video>()
        for (txt in aj.carpetas) {
            val arbol = Uri.parse(txt)
            val raiz = try { DocumentsContract.getTreeDocumentId(arbol) } catch (e: Exception) { continue }
            recorrer(ctx, arbol, raiz, nombreCarpeta(raiz), 1, aj, salida)
        }
        guardarCache(ctx, salida)
        return salida
    }

    private fun nombreCarpeta(docId: String) = docId.substringAfterLast('/').substringAfterLast(':').ifBlank { "Carpeta" }

    private fun recorrer(ctx: Context, arbol: Uri, docId: String, nombre: String, nivel: Int, aj: Ajustes, out: MutableList<Video>) {
        val hijos = DocumentsContract.buildChildDocumentsUriUsingTree(arbol, docId)
        try {
            ctx.contentResolver.query(hijos, COLUMNAS, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val nom = c.getString(1) ?: continue
                    val mime = c.getString(2) ?: ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        if (nom.startsWith(".") && !aj.ocultas) continue
                        if (aj.profundidad != 0 && nivel >= aj.profundidad) continue
                        recorrer(ctx, arbol, id, nom, nivel + 1, aj, out)
                    } else if (mime.startsWith("video/") || nom.substringAfterLast('.', "").lowercase() in EXTENSIONES) {
                        if (nom.startsWith(".") && !aj.ocultas) continue
                        out.add(Video(DocumentsContract.buildDocumentUriUsingTree(arbol, id), nom, c.getLong(3), c.getLong(4), nombre))
                    }
                }
            }
        } catch (e: Exception) {
            // Una carpeta ilegible no debe tumbar el resto del recorrido.
        }
    }

    fun ordenar(lista: List<Video>, orden: Int) = when (orden) {
        Ajustes.ORDEN_FECHA -> lista.sortedByDescending { it.fecha }
        1 -> lista.sortedBy { it.nombre.lowercase() }
        else -> lista.sortedByDescending { it.tam }
    }

    // Caché en disco: la lista aparece al instante y luego se refresca en segundo plano.
    private fun archivo(ctx: Context) = File(ctx.filesDir, "biblioteca.json")

    private fun guardarCache(ctx: Context, lista: List<Video>) {
        try {
            val a = JSONArray()
            lista.forEach { a.put(JSONObject().put("u", it.uriTxt).put("n", it.nombre).put("t", it.tam).put("f", it.fecha).put("c", it.carpeta)) }
            archivo(ctx).writeText(a.toString())
        } catch (e: Exception) {
        }
    }

    fun leerCache(ctx: Context): List<Video> = try {
        val a = JSONArray(archivo(ctx).readText())
        List(a.length()) { val o = a.getJSONObject(it); Video(Uri.parse(o.getString("u")), o.getString("n"), o.getLong("t"), o.getLong("f"), o.getString("c")) }
    } catch (e: Exception) {
        emptyList()
    }
}
