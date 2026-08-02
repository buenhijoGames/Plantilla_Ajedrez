package com.buenhijogames.plantilla_ajedrez.domain.motor

import kotlinx.coroutines.flow.Flow

/**
 * Información de evaluación del motor para una posición dada.
 *
 * @property profundidad   Depth alcanzado por el motor (a mayor, más fiable).
 * @property scoreCentipepeños  Puntaje en centipepeños (centipawns) desde el
 *                          punto de vista de las blancas.
 * @property mejorJugada    SAN de la mejor línea sugerida por el motor.
 * @property lineaPrincipal Secuencia de SANs con la variación principal (PV).
 */
data class Evaluacion(
    val profundidad: Int,
    val scoreCentipepeños: Int,
    val mejorJugada: String,
    val lineaPrincipal: List<String> = emptyList(),
)

/**
 * Puerto del motor de evaluación (Stockfish UCI). Voltio: el motor está
 * encapsulado en `:data` yrespeta el ciclo de vida del proceso; aquí la
 * interfaz solo expone lo mínimo.
 */
interface PuertoEvaluacionMotor {

    /** Inicia el subproceso UCI del motor, si es que no estaba ya activo. */
    suspend fun arrancar()

    /** Detiene el subproceso del motor y libera recursos. */
    suspend fun parar()

    /** Emite evaluaciones sucesivas a medida que el motor profundiza. */
    fun analizar(fen: String, profundidadMaxima: Int = 18): Flow<Evaluacion>

    /** Pide al motor la mejor jugada para una posición. */
    suspend fun mejorJugada(fen: String, profundidadMaxima: Int = 18): String?
}