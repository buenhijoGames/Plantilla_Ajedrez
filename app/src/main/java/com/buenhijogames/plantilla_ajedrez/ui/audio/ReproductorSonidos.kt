package com.buenhijogames.plantilla_ajedrez.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.buenhijogames.plantilla_ajedrez.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tipos de efectos de sonido disponibles al realizar jugadas en el tablero.
 */
enum class TipoSonidoJugada {
    /** Movimiento regular de una pieza a una casilla vacía. */
    MOVIMIENTO,

    /** Captura de una pieza enemiga. */
    CAPTURA,

    /** Jugada que resulta en jaque o jaque mate. */
    JAQUE,

    /** Enroque (corto/largo) o promoción de peón. */
    ENROQUE_O_PROMOCION,
}

/**
 * Gestor de reproducción de efectos de sonido de baja latencia para el ajedrez.
 *
 * Utiliza [SoundPool] para reproducir sonidos de forma inmediata sin sobrecarga.
 * Los sonidos se cargan en memoria en la inicialización y se mantienen listos.
 *
 * @param contexto Contexto de la aplicación para acceder a los recursos de audio.
 */
@Singleton
class ReproductorSonidos @Inject constructor(
    @ApplicationContext private val contexto: Context,
) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build()
        )
        .build()

    private val idMovimiento: Int = soundPool.load(contexto, R.raw.sonido_movimiento, 1)
    private val idCaptura: Int = soundPool.load(contexto, R.raw.sonido_captura, 1)
    private val idJaque: Int = soundPool.load(contexto, R.raw.sonido_jaque, 1)
    private val idEspecial: Int = soundPool.load(contexto, R.raw.sonido_especial, 1)

    /**
     * Reproduce el efecto de sonido correspondiente a [tipo].
     *
     * @param tipo Tipo de sonido a reproducir.
     */
    fun reproducir(tipo: TipoSonidoJugada) {
        val soundId = when (tipo) {
            TipoSonidoJugada.MOVIMIENTO -> idMovimiento
            TipoSonidoJugada.CAPTURA -> idCaptura
            TipoSonidoJugada.JAQUE -> idJaque
            TipoSonidoJugada.ENROQUE_O_PROMOCION -> idEspecial
        }
        if (soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }
}
