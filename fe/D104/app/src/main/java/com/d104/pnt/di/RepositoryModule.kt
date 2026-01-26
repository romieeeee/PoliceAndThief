package com.d104.pnt.di

import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.AuthRepositoryImpl
import com.d104.pnt.data.repository.WalkieRepository
import com.d104.pnt.data.repository.WalkieRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindWalkieRepository(
        impl: WalkieRepositoryImpl
    ): WalkieRepository
}
