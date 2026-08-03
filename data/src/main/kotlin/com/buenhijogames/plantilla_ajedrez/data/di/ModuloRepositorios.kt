package com.buenhijogames.plantilla_ajedrez.data.di

import com.buenhijogames.plantilla_ajedrez.data.repositorio.GeneradorIdsUuid
import com.buenhijogames.plantilla_ajedrez.data.repositorio.RelojSistema
import com.buenhijogames.plantilla_ajedrez.data.repositorio.RepositorioPartidasImpl
import com.buenhijogames.plantilla_ajedrez.data.repositorio.RepositorioTorneosImpl
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.GeneradorIds
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.Reloj
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.RepositorioPartidas
import com.buenhijogames.plantilla_ajedrez.domain.repositorio.RepositorioTorneos
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt de bindings de la capa de datos.
 *
 * Aquí se "conectan" las interfaces de dominio (puertos) con sus
 * implementaciones concretas en `:data`. Es el único punto donde la
 * composition root de DI conoce ambas capas, respetando DIP: ninguno de los
 * consumidores (presentación o casos de uso) sabe que detrás hay Room ni
 * estos Implementadores concretos.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ModuloRepositorios {

    @Binds
    @Singleton
    abstract fun bindRepositorioTorneos(impl: RepositorioTorneosImpl): RepositorioTorneos

    @Binds
    @Singleton
    abstract fun bindRepositorioPartidas(impl: RepositorioPartidasImpl): RepositorioPartidas

    @Binds
    @Singleton
    abstract fun bindGeneradorIds(impl: GeneradorIdsUuid): GeneradorIds

    @Binds
    @Singleton
    abstract fun bindReloj(impl: RelojSistema): Reloj
}