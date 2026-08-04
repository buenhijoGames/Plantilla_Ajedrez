package com.buenhijogames.plantilla_ajedrez.domain.motor

import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida

/**
 * Puerto de motor de ajedrez: validación legal, conversión a SAN, FEN y estado.
 *
 * Implementación concreta: `:data` la provee con chesslib (Apache 2.0). Esto
 *permite abstraer la API de chesslib tras una interfaz de dominio y, por
 * ende, poder cambiar de librería (o implementar la lógica a mano) en el
 * futuro sin tocar la capa de presentación.
 */
interface PuertoMotorAjedrez {

    /** Aplica [san] a la posición actual dada por [fen] y retorna el nuevo FEN. */
    fun aplicarJugada(fen: String, san: String): String

    /** Genera SAN a partir del movimento origen -> destino en notación simple. */
    fun jugadaASan(fen: String, desde: String, hasta: String, promocion: Char? = null): String

    /** Lista de casillas destino legales desde [desde]. */
    fun jugadasLegalesDesde(fen: String, desde: String): List<String>

    /** Devuelve el FEN en su forma estándar de posición inicial. */
    fun fenInicial(): String

    /** Indica si [fen] corresponde a posición de final: jaque mate o ahogado. */
    fun esFinal(fen: String): Boolean

    /** Comprueba si [fen] corresponde a tablas por regla de 50 jugadas o triple repetición. */
    fun esTablas(fen: String): Boolean

    /**
     * Calcula el [ResultadoPartida] actual de la posición [fen].
     *
     * Diferencia entre jaque mate (gana el bando que no está en mate),
     * ahogado, tablas reglamentarias y partida en curso. Es la fuente de
     * verdad del resultado para la UI cuando el usuario termina la partida.
     */
    fun resultadoActual(fen: String): ResultadoPartida
}