package com.video.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.TextUtils
import android.text.format.Formatter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Galería solo de video: muestra únicamente las carpetas elegidas en "Carpetas". Mantén presionado para seleccionar. */
class MainActivity : Activity() {
    private var todos: List<Video> = emptyList()
    private var visibles: List<Video> = emptyList()
    private var filtro: String? = null
    private val seleccion = LinkedHashSet<String>()
    private var temaAplicado = -1
    private lateinit var aj: Ajustes
    private lateinit var cuadricula: GridView
    private lateinit var vacio: TextView
    private lateinit var chips: LinearLayout
    private lateinit var estado: TextView
    private lateinit var barraNormal: LinearLayout
    private lateinit var barraSeleccion: LinearLayout
    private lateinit var contador: TextView
    private val adaptador = Adaptador()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        aj = Ajustes(this)
        Ui.aplicar(this)
        temaAplicado = aj.tema
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        val d16 = Ui.dp(this, 16)

        barraNormal = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(d16, Ui.dp(context, 12), Ui.dp(context, 8), Ui.dp(context, 4))
            addView(Ui.texto(context, "Video", 22f, Ui.ROJO, true), LinearLayout.LayoutParams(0, -2, 1f))
            addView(Ui.boton(context, "↻") { actualizar() })
            addView(Ui.boton(context, "Carpetas") { startActivity(Intent(context, CarpetasActivity::class.java)) })
            addView(Ui.boton(context, "Ajustes") { startActivity(Intent(context, AjustesActivity::class.java)) })
        }
        contador = Ui.texto(this, "", 16f, Ui.ROJO, true)
        barraSeleccion = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(Ui.dp(context, 8), Ui.dp(context, 12), Ui.dp(context, 4), Ui.dp(context, 4))
            addView(Ui.boton(context, "✕") { limpiarSeleccion() })
            addView(contador, LinearLayout.LayoutParams(0, -2, 1f))
            addView(Ui.boton(context, "Todos") { seleccionarTodos() })
            addView(Ui.boton(context, "Compartir") { compartir() })
            addView(Ui.boton(context, "Borrar") { confirmarBorrado() })
        }
        chips = LinearLayout(this).apply { setPadding(Ui.dp(context, 12), 0, Ui.dp(context, 12), Ui.dp(context, 8)) }
        estado = Ui.texto(this, "", 11f, Ui.TENUE).apply { setPadding(d16, 0, 0, Ui.dp(context, 4)) }
        vacio = Ui.texto(this, "", 15f).apply {
            gravity = Gravity.CENTER
            setPadding(Ui.dp(context, 32), Ui.dp(context, 32), Ui.dp(context, 32), Ui.dp(context, 32))
        }
        cuadricula = GridView(this).apply {
            numColumns = 2
            adapter = adaptador
            horizontalSpacing = Ui.dp(context, 8)
            verticalSpacing = Ui.dp(context, 8)
            setPadding(Ui.dp(context, 12), 0, Ui.dp(context, 12), Ui.dp(context, 12))
            clipToPadding = false
            setOnItemClickListener { _, _, pos, _ -> if (seleccion.isEmpty()) reproducir(pos) else alternar(visibles[pos]) }
            setOnItemLongClickListener { _, _, pos, _ -> alternar(visibles[pos]); true }
        }
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.FONDO)
            addView(barraNormal)
            addView(barraSeleccion)
            addView(HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; addView(chips) })
            addView(estado)
            addView(vacio)
            addView(cuadricula, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        Ui.insets(raiz)
        setContentView(raiz)

        todos = Escaner.leerCache(this)
        aplicarFiltro()
    }

    override fun onResume() {
        super.onResume()
        if (aj.tema != temaAplicado) {
            recreate()
            return
        }
        actualizar()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (seleccion.isNotEmpty()) limpiarSeleccion() else super.onBackPressed()
    }

    private fun actualizar() {
        if (aj.carpetas.isEmpty()) {
            todos = emptyList()
            aplicarFiltro()
            return
        }
        estado.text = "Buscando videos…"
        Thread {
            val lista = Escaner.escanear(applicationContext, aj)
            runOnUiThread {
                todos = lista
                if (filtro != null && lista.none { it.carpeta == filtro }) filtro = null
                seleccion.retainAll(lista.map { it.uriTxt }.toSet())
                aplicarFiltro()
            }
        }.start()
    }

    private fun aplicarFiltro() {
        val base = if (filtro == null) todos else todos.filter { it.carpeta == filtro }
        visibles = Escaner.ordenar(base, aj.orden)
        cuadricula.visibility = if (visibles.isEmpty()) View.GONE else View.VISIBLE
        vacio.visibility = if (visibles.isEmpty()) View.VISIBLE else View.GONE
        vacio.text = if (aj.carpetas.isEmpty()) {
            "Todavía no elegiste carpetas.\n\nToca «Carpetas» y elige dónde buscar videos. La app no recorre el resto del celular."
        } else {
            "No se encontraron videos en las carpetas elegidas."
        }
        armarChips()
        refrescarBarra()
        adaptador.notifyDataSetChanged()
    }

    private fun refrescarBarra() {
        val enSeleccion = seleccion.isNotEmpty()
        barraNormal.visibility = if (enSeleccion) View.GONE else View.VISIBLE
        barraSeleccion.visibility = if (enSeleccion) View.VISIBLE else View.GONE
        contador.text = "${seleccion.size} seleccionados"
        estado.text = if (todos.isEmpty()) "" else "${visibles.size} videos" + if (!enSeleccion) " · mantén presionado para seleccionar" else ""
    }

    private fun armarChips() {
        chips.removeAllViews()
        val nombres = todos.map { it.carpeta }.distinct()
        if (nombres.size < 2) return
        for (n in listOf<String?>(null) + nombres) {
            val activo = n == filtro
            chips.addView(
                Ui.texto(this, n ?: "Todos", 12f, if (activo) Ui.FONDO else Ui.VERDE, activo).apply {
                    setPadding(Ui.dp(context, 14), Ui.dp(context, 6), Ui.dp(context, 14), Ui.dp(context, 6))
                    background = GradientDrawable().apply {
                        cornerRadius = Ui.dp(context, 16).toFloat()
                        if (activo) setColor(Ui.VERDE) else setStroke(2, Ui.TENUE)
                    }
                    setOnClickListener { filtro = n; aplicarFiltro() }
                },
                LinearLayout.LayoutParams(-2, -2).apply { setMargins(0, 0, Ui.dp(this@MainActivity, 8), 0) },
            )
        }
    }

    private fun alternar(v: Video) {
        if (!seleccion.add(v.uriTxt)) seleccion.remove(v.uriTxt)
        refrescarBarra()
        adaptador.notifyDataSetChanged()
    }

    private fun limpiarSeleccion() {
        seleccion.clear()
        refrescarBarra()
        adaptador.notifyDataSetChanged()
    }

    private fun seleccionarTodos() {
        visibles.forEach { seleccion.add(it.uriTxt) }
        refrescarBarra()
        adaptador.notifyDataSetChanged()
    }

    private fun elegidos() = todos.filter { it.uriTxt in seleccion }

    private fun compartir() {
        val lista = elegidos()
        if (lista.isEmpty()) return
        val uris = ArrayList<Uri>(lista.map { it.uri })
        val i = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
        i.type = "video/*"
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        i.clipData = ClipData.newRawUri("videos", uris[0]).also { c -> uris.drop(1).forEach { c.addItem(ClipData.Item(it)) } }
        try {
            startActivity(Intent.createChooser(i, "Compartir ${uris.size} video(s)"))
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo compartir: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmarBorrado() {
        val lista = elegidos()
        if (lista.isEmpty()) return
        val peso = Formatter.formatShortFileSize(this, lista.sumOf { it.tam })
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("¿Borrar ${lista.size} video(s)?")
            .setMessage("Se eliminan del celular ($peso). No van a una papelera y no se pueden recuperar.")
            .setPositiveButton("Borrar") { _, _ -> borrar(lista) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun borrar(lista: List<Video>) {
        estado.text = "Borrando…"
        Thread {
            var bien = 0
            for (v in lista) {
                val ok = try { DocumentsContract.deleteDocument(contentResolver, v.uri) } catch (e: Exception) { false }
                if (ok) bien++
            }
            runOnUiThread {
                seleccion.clear()
                val fallos = lista.size - bien
                val msg = "Borrados: $bien" + if (fallos > 0) " · No se pudieron borrar: $fallos (vuelve a agregar esa carpeta en «Carpetas» para dar permiso de borrado)" else ""
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                actualizar()
            }
        }.start()
    }

    private fun reproducir(pos: Int) {
        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putStringArrayListExtra("uris", ArrayList(visibles.map { it.uriTxt }))
                .putStringArrayListExtra("nombres", ArrayList(visibles.map { it.nombre }))
                .putExtra("indice", pos),
        )
    }

    private inner class Adaptador : BaseAdapter() {
        override fun getCount() = visibles.size
        override fun getItem(i: Int) = visibles[i]
        override fun getItemId(i: Int) = i.toLong()
        override fun getView(i: Int, vista: View?, padre: ViewGroup): View {
            val ctx = this@MainActivity
            val tarjeta = (vista as? LinearLayout) ?: LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                val marco = FrameLayout(ctx).apply {
                    background = GradientDrawable().apply { setColor(Ui.SUPERFICIE); cornerRadius = Ui.dp(ctx, 10).toFloat() }
                    clipToOutline = true
                    addView(ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP }, FrameLayout.LayoutParams(-1, -1))
                    addView(
                        Ui.texto(ctx, "✓", 18f, Ui.FONDO, true).apply {
                            gravity = Gravity.CENTER
                            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Ui.ROJO) }
                        },
                        FrameLayout.LayoutParams(Ui.dp(ctx, 28), Ui.dp(ctx, 28), Gravity.TOP or Gravity.END).apply { setMargins(0, Ui.dp(ctx, 6), Ui.dp(ctx, 6), 0) },
                    )
                }
                addView(marco, LinearLayout.LayoutParams(-1, Ui.dp(ctx, 110)))
                addView(Ui.texto(ctx, "", 12f, Ui.TEXTO).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END })
                addView(Ui.texto(ctx, "", 10f, Ui.TENUE))
            }
            val v = visibles[i]
            val marco = tarjeta.getChildAt(0) as FrameLayout
            val elegido = v.uriTxt in seleccion
            Miniaturas.cargar(ctx, v, marco.getChildAt(0) as ImageView)
            marco.getChildAt(1).visibility = if (elegido) View.VISIBLE else View.GONE
            marco.foreground = if (elegido) ColorDrawable((Ui.ROJO and 0x00FFFFFF) or 0x55000000) else null
            (tarjeta.getChildAt(1) as TextView).text = v.nombre
            (tarjeta.getChildAt(2) as TextView).text = Formatter.formatShortFileSize(ctx, v.tam)
            return tarjeta
        }
    }
}
