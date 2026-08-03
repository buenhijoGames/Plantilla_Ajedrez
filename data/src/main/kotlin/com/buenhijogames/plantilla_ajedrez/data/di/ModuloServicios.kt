package com.buenhijogames.plantilla_ajedrez.data.di

import com.buenhijogames.plantilla_ajedrez.data.ajedrez.AdaptadorChesslib
import com.buenhijogames.plantilla_ajedrez.data.pgn.AdaptadorPgn
import com.buenhijogames.plantilla_ajedrez.domain.motor.PuertoMotorAjedrez
import com.buenhijogames.plantilla_ajedrez.domain.pgn.PuertoPgn
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Modulo Hilt de servicios de dominio (motor de ajedrez y PGN).
 *
 * Aqui se "conectan" los puertos de dominio con sus implementaciones
 * concretas en `:data`. Separamos este modulo del [ModuloRepositorios]
 * por responsabilidad unica: repositorios vs servicios stateless.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ModuloServicios {

    @Binds
    @Singleton
    abstract fun bindPuertoMotorAjedrez(impl: AdaptadorChesslib): PuertoMotorAjedrez

    @Binds
    @Singleton
    abstract fun bindPuertoPgn(impl: AdaptadorPgn): PuertoPgn
}