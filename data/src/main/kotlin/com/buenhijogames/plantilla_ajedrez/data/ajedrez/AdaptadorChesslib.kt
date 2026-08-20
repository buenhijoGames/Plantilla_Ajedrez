package com.buenhijogames.plantilla_ajedrez.data.ajedrez

import com.buenhijogames.plantilla_ajedrez.domain.modelo.ResultadoPartida
import com.buenhijogames.plantilla_ajedrez.domain.motor.PuertoMotorAjedrez
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.github.bhlangonijr.chesslib.move.MoveList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion de [PuertoMotorAjedrez] respaldada por chesslib (Apache 2.0).
 *
 * Cada operacion crea un [Board] efimero cargado desde el FEN recibido. Esto
 * hace que el adaptador sea **sin estado**: no depende de mutacion compartida
 * y es seguro llamarlo desde multiples corutinas. El coste de crear un Board
 * por operacion es despreciable porque solo carga el FEN.
 *
 * Manejo de errores:
 *   - Los FEN invalidos se envuelven en [IllegalStateException] para contexto.
 *   - Las jugadas ilegales se rechazan con [JugadaIlegalException] propia,
 *     para que la UI la trate como validacion de usuario (no como crash).
 *
 * Notacion:
 *   - [desde] y [hasta] se pasan en notacion algebraica simple ("e2", "e4"),
 *     en minusculas o mayusculas. Internamente se normaliza a mayusculas
 *     porque [Square.fromValue] requiere nombres de enum exactos (E2, E4).
 *   - [promocion] recibe el simbolo de la pieza (Q,R,B,N, mayuscula). El lado
 *     de la pieza se ajusta segun el lado al que le toca mover en el FEN.
 */
@Singleton
class AdaptadorChesslib @Inject constructor() : PuertoMotorAjedrez {

    /** FEN de la posicion inicial estandar de ajedrez. */
    override fun fenInicial(): String = FEN_INICIAL_ESTANDAR

    /**
     * Aplica [san] al FEN dado y devuelve el nuevo FEN.
     *
     * @throws JugadaIlegalException si [san] no es valido o ilegal.
     */
    override fun aplicarJugada(fen: String, san: String): String {
        val board = Board().cargar(fen)
        val ok = try {
            board.doMove(san)
        } catch (e: RuntimeException) {
            // chesslib lanza MoveConversionException (runtime) cuando el SAN
            // no es parseable o la jugada no es legal. La envolvemos en
            // JugadaIlegalException para que la UI la trate como validacion.
            throw JugadaIlegalException("Jugada no valida: $san", e)
        }
        if (!ok) throw JugadaIlegalException("Jugada ilegal: $san")
        return board.getFen()
    }

    /**
     * Genera SAN a partir del movimiento origen -> destino.
     *
     * @param promocion Simbolo de pieza promocionada (Q,R,B,N mayuscula) o null.
     * @throws JugadaIlegalException si la jugada no es legal en la posicion.
     */
    override fun jugadaASan(
        fen: String,
        desde: String,
        hasta: String,
        promocion: Char?,
    ): String {
        val board = Board().cargar(fen)
        val origen = Square.fromValue(desde.normalizarCasilla())
        val destino = Square.fromValue(hasta.normalizarCasilla())
        val piezaPromo = promocion?.aPiece(board.getSideToMove())
        val move = if (piezaPromo != null) Move(origen, destino, piezaPromo) else Move(origen, destino)

        // Validamos legalidad real de ajedrez: [Board.isMoveLegal] (con
        // fullValidation=true) solo asegura que la posicion resultante no
        // deja al propio rey en jaque, pero permite movimientos que infringen
        // reglas de movimiento (rey dos casillas, peon tres casillas, ...).
        // Por eso contrastamos con los movimientos legales generados por
        // [MoveGenerator.generateLegalMoves].
        val legales = try {
            MoveGenerator.generateLegalMoves(board)
        } catch (e: Exception) {
            emptyList<Move>()
        }
        if (move !in legales) {
            throw JugadaIlegalException(
                "Jugada ilegal: $desde-$hasta${promocion?.let { "=$it" } ?: ""}"
            )
        }

        // Para obtener el SAN correcto, dejamos que [MoveList] aplique la
        // jugada desde el FEN inicial y compute la notacion algebraica.
        // Usamos el ultimo elemento del array de SANs generado.
        val moves = MoveList(fen)
        moves.add(move)
        return try {
            moves.toSanArray().last()
        } catch (e: Exception) {
            // En caso de fallo de conversion, devolvemos la notacion
            // compacta desde-hasta (menos amigable pero no rompe la app).
            "$desde$hasta${promocion?.let { "=$it" } ?: ""}"
        }
    }

    /**
     * Devuelve las casillas destino legales a las que puede mover la pieza
     * en [desde]. Vacia si no hay pieza movil o ninguna jugada es legal.
     */
    override fun jugadasLegalesDesde(fen: String, desde: String): List<String> {
        val board = Board().cargar(fen)
        val origen = Square.fromValue(desde.normalizarCasilla())
        if (board.getPiece(origen) == Piece.NONE) return emptyList()
        val legales = try {
            MoveGenerator.generateLegalMoves(board)
        } catch (e: Exception) {
            return emptyList()
        }
        return legales.filter { it.getFrom() == origen }.map { it.getTo().name.lowercase() }
    }

    /**
     * Indica si [fen] corresponde a fin de partida por jaque mate o ahogado.
     */
    override fun esFinal(fen: String): Boolean {
        val board = Board().cargar(fen)
        return board.isMated || board.isStaleMate
    }

    /**
     * Comprueba si [fen] corresponde a tablas por regla de 50 jugadas,
     * triple repeticion o material insuficiente.
     */
    override fun esTablas(fen: String): Boolean {
        val board = Board().cargar(fen)
        return board.isDraw || board.isInsufficientMaterial || board.isRepetition()
    }

    /**
     * Calcula el [ResultadoPartida] actual de la posicion [fen].
     *
     * Si el lado al que le toca mover esta en mate, gana el bando contrario;
     * el ahogado y el resto de tablas reglamentarias devuelven [ResultadoPartida.TABLAS];
     * en cualquier otro caso la partida sigue en curso.
     */
    override fun resultadoActual(fen: String): ResultadoPartida {
        val board = Board().cargar(fen)
        val sinJugadasLegales = board.legalMoves().isEmpty()
        return when {
            sinJugadasLegales && board.isKingAttacked ->
                if (board.getSideToMove() == Side.WHITE) ResultadoPartida.GANA_NEGRAS
                else ResultadoPartida.GANA_BLANCAS
            sinJugadasLegales || board.isDraw || board.isInsufficientMaterial || board.isRepetition() ->
                ResultadoPartida.TABLAS
            else -> ResultadoPartida.EN_CURSO
        }
    }

    companion object {
        /** FEN de la posicion inicial estandar. */
        const val FEN_INICIAL_ESTANDAR: String =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    }
}

/**
 * Excepcion para rechazar jugadas ilegales en la capa de datos.
 *
 * Se usa en lugar de [IllegalArgumentException] para que la UI la trate
 * como validacion controlada del usuario (no como crash de la app).
 */
class JugadaIlegalException(mensaje: String, causa: Throwable? = null) : RuntimeException(mensaje, causa)

/**
 * Extension interna para cargar un [Board] desde FEN envolviendo la
 * conversion accidental de FEN invalido en una [IllegalStateException]
 * con contexto para diagnostico.
 */
private fun Board.cargar(fen: String): Board = try {
    loadFromFen(fen)
    this
} catch (e: IllegalArgumentException) {
    throw IllegalStateException("FEN no valido: '$fen'", e)
}

/**
 * Normaliza la notacion de una casilla a mayusculas (formato esperado por
 * [Square.fromValue]). Se acepta "e2" o "E2", se devuelve "E2".
 */
private fun String.normalizarCasilla(): String = this.uppercase()

/**
 * Convierte un Char de promocion (Q,R,B,N mayuscula o minuscula) en
 * [Piece] de chesslib respetando el lado a mover.
 */
private fun Char.aPiece(lado: Side): Piece {
    val simbolo = if (lado == Side.WHITE) this.uppercaseChar() else this.lowercaseChar()
    return Piece.fromFenSymbol(simbolo.toString())
}