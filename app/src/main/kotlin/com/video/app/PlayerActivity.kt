@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.video.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.ListenableFuture

/** Pantalla de reproducción. El reproductor vive en PlayerService; aquí solo se controla y se muestra. */
class PlayerActivity : Activity() {
    private lateinit var vista: PlayerView
    private lateinit var diag: TextView
    private lateinit var rotar: TextView
    private var futuro: ListenableFuture<MediaController>? = null
    private var control: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refresco = object : Runnable {
        override fun run() {
            diag.text = Diag.texto()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(estado: Bundle?) {
        super.onCreate(estado)
        Ui.aplicar(this)
        vista = PlayerView(this).apply {
            setBackgroundColor(Color.BLACK)
            controllerShowTimeoutMs = 3000
        }
        diag = TextView(this).apply {
            setTextColor(Ui.VERDE)
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
            setBackgroundColor(0x99000000.toInt())
            setPadding(16, 8, 16, 8)
            visibility = View.GONE
        }
        rotar = Ui.texto(this, "", 13f, Ui.VERDE, true).apply {
            setBackgroundColor(0x99000000.toInt())
            setPadding(24, 14, 24, 14)
            setOnClickListener { cambiarRotacion() }
        }
        aplicarRotacion()
        vista.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v -> rotar.visibility = if (v == View.VISIBLE) View.VISIBLE else View.GONE })
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(vista, FrameLayout.LayoutParams(-1, -1))
            addView(diag, FrameLayout.LayoutParams(-2, -2).apply { setMargins(24, 48, 0, 0) })
            addView(rotar, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply { setMargins(0, 48, 24, 0) })
        })

        val gestos = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val c = control ?: return false
                if (e.x < vista.width / 2f) c.seekBack() else c.seekForward()
                return true
            }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (vista.isControllerFullyVisible) vista.hideController() else vista.showController()
                return true
            }
        })
        vista.setOnTouchListener { _, ev -> gestos.onTouchEvent(ev) }

        modoInmersivo()
        mantenerPantalla(true)
        conectar(estado == null)
    }

    /** Auto usa el sensor aunque el bloqueo de rotación del sistema esté puesto; Horizontal y Vertical fuerzan la orientación. */
    private fun aplicarRotacion() {
        val modo = Ajustes(this).rotacion
        requestedOrientation = when (modo) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            2 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
        rotar.text = "⟳ " + listOf("Auto", "Horizontal", "Vertical")[modo.coerceIn(0, 2)]
    }

    private fun cambiarRotacion() {
        val aj = Ajustes(this)
        aj.rotacion = (aj.rotacion + 1) % 3
        aplicarRotacion()
    }

    /** La pantalla no se apaga sola mientras se reproduce; al pausar o terminar vuelve el tiempo de espera normal del sistema. */
    private fun mantenerPantalla(si: Boolean) {
        if (si) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun modoInmersivo() {
        window.setDecorFitsSystemWindows(false)
        window.insetsController?.let {
            it.hide(WindowInsets.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun conectar(cargar: Boolean) {
        val token = SessionToken(this, ComponentName(this, PlayerService::class.java))
        val f = MediaController.Builder(this, token).buildAsync()
        futuro = f
        f.addListener({
            val c = try { f.get() } catch (e: Exception) { finish(); return@addListener }
            control = c
            vista.player = c
            c.addListener(object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = mantenerPantalla(playWhenReady)
                override fun onPlaybackStateChanged(estado: Int) {
                    if (estado == Player.STATE_ENDED) mantenerPantalla(false)
                }
            })
            if (cargar) reproducir(c)
            if (Ajustes(this).diagnostico) {
                diag.visibility = View.VISIBLE
                handler.post(refresco)
            }
        }, mainExecutor)
    }

    private fun reproducir(c: MediaController) {
        val aj = Ajustes(this)
        val uris = intent.getStringArrayListExtra("uris")
        val nombres = intent.getStringArrayListExtra("nombres")
        val items = if (uris != null) {
            uris.mapIndexed { i, u -> item(Uri.parse(u), nombres?.getOrNull(i)) }
        } else {
            listOf(item(intent.data ?: return, null))
        }
        c.setMediaItems(items, intent.getIntExtra("indice", 0).coerceIn(0, items.size - 1), 0)
        c.setPlaybackSpeed(aj.velocidad / 100f)
        c.prepare()
        c.playWhenReady = true
    }

    private fun item(uri: Uri, titulo: String?) = MediaItem.Builder()
        .setUri(uri)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(MediaMetadata.Builder().setTitle(titulo ?: uri.lastPathSegment?.substringAfterLast('/')).build())
        .build()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        control?.let { reproducir(it) }
    }

    override fun onStop() {
        super.onStop()
        // Con "segundo plano" apagado, salir de la pantalla pausa. Encendido, el servicio sigue reproduciendo.
        if (!Ajustes(this).segundoPlano) control?.pause()
    }

    /** Atrás = salir de verdad: detiene la reproducción y cierra el servicio. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        control?.stop()
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresco)
        vista.player = null
        futuro?.let { MediaController.releaseFuture(it) }
        if (isFinishing) stopService(Intent(this, PlayerService::class.java))
        super.onDestroy()
    }
}
