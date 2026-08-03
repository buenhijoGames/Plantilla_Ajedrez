package com.buenhijogames.plantilla_ajedrez.data.repositorio

import com.buenhijogames.plantilla_ajedrez.domain.repositorio.GeneradorIds
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.Reloj
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de producción de [GeneradorIds] basada en [UUID].
 */
@Singleton
class GeneradorIdsUuid @Inject constructor() : GeneradorIds {
    override fun nuevoId(): String = UUID.randomUUID().toString()
}

/**
 * Implementación de producción de [Reloj] basada en el reloj del sistema.
 */
@Singleton
class RelojSistema @Inject constructor() : Reloj {
    override fun ahora(): Long = System.currentTimeMillis()
}