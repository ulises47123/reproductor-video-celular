@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.video.app

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

/** Lo último que midió el reproductor: qué decodificador usó, cuánto tardó el primer cuadro y cuántos cuadros se perdieron. */
object Diag {
    @Volatile var decodificador = "—"
    @Volatile var formato = "—"
    @Volatile var primerCuadroMs = -1L
    @Volatile var caidos = 0L
    @Volatile var inicioMs = 0L

    fun reiniciar() { decodificador = "—"; formato = "—"; primerCuadroMs = -1L; caidos = 0L; inicioMs = SystemClock.elapsedRealtime() }

    fun texto(): String {
        val hw = if (decodificador.startsWith("c2.android") || decodificador.startsWith("OMX.google")) "SOFTWARE" else if (decodificador == "—") "" else "HARDWARE"
        return "decodificador: $decodificador $hw\nformato: $formato\n" +
            "primer cuadro: ${if (primerCuadroMs >= 0) "$primerCuadroMs ms" else "—"}\ncuadros perdidos: $caidos"
    }
}

/** Ordena los decodificadores disponibles según la política elegida y el decodificador fijado por códec. */
class SelectorCodec(private val aj: Ajustes) : MediaCodecSelector {
    override fun getDecoderInfos(mimeType: String, requiresSecureDecoder: Boolean, requiresTunnelingDecoder: Boolean): List<MediaCodecInfo> {
        val todos = MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
        fun esHw(i: MediaCodecInfo) = i.hardwareAccelerated && !i.softwareOnly
        val ordenados = when (aj.politica) {
            Ajustes.POL_SOLO_HW -> todos.filter { esHw(it) }
            Ajustes.POL_HW_PRIMERO -> todos.sortedBy { if (esHw(it)) 0 else 1 }
            Ajustes.POL_SW_PRIMERO -> todos.sortedBy { if (esHw(it)) 1 else 0 }
            else -> todos
        }
        val fijado = aj.decoderFijado(mimeType) ?: return ordenados
        return ordenados.sortedBy { if (it.name == fijado) 0 else 1 }
    }
}

object Motor {
    fun crear(ctx: Context): ExoPlayer {
        val aj = Ajustes(ctx)
        val fabrica = DefaultRenderersFactory(ctx)
            .setEnableDecoderFallback(aj.fallback)
            .setMediaCodecSelector(SelectorCodec(aj))
        when (aj.colaMediaCodec) {
            1 -> fabrica.forceEnableMediaCodecAsynchronousQueueing()
            2 -> fabrica.forceDisableMediaCodecAsynchronousQueueing()
        }
        val b = Ajustes.BUFFER_MS[aj.buffer.coerceIn(0, 2)]
        val carga = DefaultLoadControl.Builder().setBufferDurationsMs(b[0], b[1], b[2], b[3]).build()

        val selector = DefaultTrackSelector(ctx)
        val parametros = selector.buildUponParameters()
            .setTunnelingEnabled(aj.tunel)
            .setAudioOffloadPreferences(
                TrackSelectionParameters.AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(if (aj.ofloadAudio) TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED else TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
                    .setIsGaplessSupportRequired(false)
                    .build(),
            )
            .build()

        val player = ExoPlayer.Builder(ctx, fabrica)
            .setTrackSelector(selector)
            .setLoadControl(carga)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
        player.trackSelectionParameters = parametros
        player.skipSilenceEnabled = aj.saltarSilencios
        player.addAnalyticsListener(medidor)
        return player
    }

    private val medidor = object : AnalyticsListener {
        override fun onMediaItemTransition(eventTime: AnalyticsListener.EventTime, mediaItem: MediaItem?, reason: Int) = Diag.reiniciar()
        override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
            Diag.decodificador = "$decoderName (${initializationDurationMs} ms)"
        }
        override fun onVideoInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {
            val fps = if (format.frameRate > 0) " ${"%.0f".format(format.frameRate)}fps" else ""
            val kbps = if (format.bitrate > 0) " ${format.bitrate / 1000}kbps" else ""
            Diag.formato = "${format.sampleMimeType?.removePrefix("video/")} ${format.width}x${format.height}$fps$kbps"
        }
        override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
            Diag.primerCuadroMs = SystemClock.elapsedRealtime() - Diag.inicioMs
        }
        override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
            Diag.caidos += droppedFrames
        }
    }
}
