package com.video.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView

/** Ajustes agrupados: aspecto, códecs, reproducción y biblioteca. */
class AjustesActivity : Activity() {
    private lateinit var aj: Ajustes
    private lateinit var cuerpo: LinearLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        aj = Ajustes(this)
        Ui.aplicar(this)
        cuerpo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.FONDO)
            addView(Ui.texto(context, "Ajustes", 22f, Ui.ROJO, true).apply { setPadding(Ui.dp(context, 16), Ui.dp(context, 12), 0, Ui.dp(context, 4)) })
            addView(ScrollView(context).apply { addView(cuerpo) }, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        Ui.insets(raiz)
        setContentView(raiz)
        pintar()
    }

    private fun pintar() {
        cuerpo.removeAllViews()
        val c = this
        fun add(v: android.view.View) = cuerpo.addView(v)

        add(Ui.titulo(c, "ASPECTO"))
        add(Ui.fila(c, "Tema", Ui.TEMAS[aj.tema.coerceIn(0, Ui.TEMAS.size - 1)]) {
            Ui.elegir(c, "Tema", Ui.TEMAS, aj.tema) { aj.tema = it; recreate() }
        })

        add(Ui.titulo(c, "CÓDECS Y DECODIFICACIÓN"))
        add(Ui.fila(c, "Política de decodificador", Ajustes.POLITICAS[aj.politica]) {
            Ui.elegir(c, "Política de decodificador", Ajustes.POLITICAS, aj.politica) { aj.politica = it; pintar() }
        })
        add(Ui.fila(c, "Elegir decodificador por códec", "Fijar uno concreto para H.264, HEVC, VP9, AV1…") {
            startActivity(Intent(c, CodecsActivity::class.java))
        })
        add(Ui.fila(c, "Cola de MediaCodec", Ajustes.COLAS[aj.colaMediaCodec]) {
            Ui.elegir(c, "Cola de MediaCodec", Ajustes.COLAS, aj.colaMediaCodec) { aj.colaMediaCodec = it; pintar() }
        })
        add(Ui.interruptor(c, "Respaldo de decodificador", "Si el elegido falla, prueba el siguiente en vez de dar error", aj.fallback) { aj.fallback = it })
        add(Ui.fila(c, "Búfer de lectura", Ajustes.BUFFERS[aj.buffer]) {
            Ui.elegir(c, "Búfer de lectura", Ajustes.BUFFERS, aj.buffer) { aj.buffer = it; pintar() }
        })
        add(Ui.interruptor(c, "Túnel de video (experimental)", "Menos CPU en algunos chips; puede dar pantalla negra", aj.tunel) { aj.tunel = it })

        add(Ui.titulo(c, "REPRODUCCIÓN"))
        add(Ui.interruptor(c, "Seguir con la pantalla apagada", "El audio continúa con controles en la notificación", aj.segundoPlano) { aj.segundoPlano = it })
        add(Ui.interruptor(c, "Audio en el chip dedicado", "Ahorra batería con la pantalla apagada (offload)", aj.ofloadAudio) { aj.ofloadAudio = it })
        add(Ui.interruptor(c, "Saltar silencios", "Acorta los momentos sin sonido", aj.saltarSilencios) { aj.saltarSilencios = it })
        add(Ui.fila(c, "Velocidad al empezar", Ajustes.VELOCIDADES.firstOrNull { it.second == aj.velocidad }?.first ?: "1x") {
            Ui.elegir(c, "Velocidad al empezar", Ajustes.VELOCIDADES.map { it.first }, Ajustes.VELOCIDADES.indexOfFirst { it.second == aj.velocidad }) {
                aj.velocidad = Ajustes.VELOCIDADES[it].second
                pintar()
            }
        })
        add(Ui.interruptor(c, "Mostrar diagnóstico", "Decodificador usado, formato, primer cuadro y cuadros perdidos", aj.diagnostico) { aj.diagnostico = it })

        add(Ui.titulo(c, "BIBLIOTECA"))
        add(Ui.fila(c, "Orden", Ajustes.ORDENES[aj.orden]) {
            Ui.elegir(c, "Orden", Ajustes.ORDENES, aj.orden) { aj.orden = it; pintar() }
        })
        add(Ui.fila(c, "Profundidad de subcarpetas", Ajustes.PROFUNDIDADES.firstOrNull { it.second == aj.profundidad }?.first ?: "Sin límite") {
            Ui.elegir(c, "Profundidad", Ajustes.PROFUNDIDADES.map { it.first }, Ajustes.PROFUNDIDADES.indexOfFirst { it.second == aj.profundidad }) {
                aj.profundidad = Ajustes.PROFUNDIDADES[it].second
                pintar()
            }
        })
        add(Ui.interruptor(c, "Incluir carpetas ocultas", "Las que empiezan con punto, como .thumbnails", aj.ocultas) { aj.ocultas = it })

        add(Ui.texto(c, "Los cambios de códecs se aplican al abrir un video nuevo. Sal del reproductor con «Atrás» para que el motor se reinicie.", 11f, Ui.TENUE).apply {
            setPadding(Ui.dp(c, 16), Ui.dp(c, 24), Ui.dp(c, 16), Ui.dp(c, 24))
        })
    }
}
