@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.video.app

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Dueño del reproductor. Al ser un servicio en primer plano, el audio sigue con la pantalla apagada
 * y la notificación trae los controles.
 */
class PlayerService : MediaSessionService() {
    private var sesion: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val reproductor = Motor.crear(this)
        sesion = MediaSession.Builder(this, reproductor)
            .setCallback(object : MediaSession.Callback {
                // El controlador manda el URI en requestMetadata; lo devolvemos a localConfiguration.
                override fun onAddMediaItems(
                    mediaSession: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: MutableList<MediaItem>,
                ): ListenableFuture<MutableList<MediaItem>> = Futures.immediateFuture(
                    mediaItems.map { it.buildUpon().setUri(it.requestMetadata.mediaUri ?: it.localConfiguration?.uri).build() }.toMutableList(),
                )
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = sesion

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = sesion?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        sesion?.run {
            player.release()
            release()
        }
        sesion = null
        super.onDestroy()
    }
}
