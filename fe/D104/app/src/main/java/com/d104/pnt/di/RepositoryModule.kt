package com.d104.pnt.di

import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.AuthRepositoryImpl
import com.d104.pnt.data.repository.ChatRepository
import com.d104.pnt.data.repository.ChatRepositoryImpl
import com.d104.pnt.data.repository.GameRoomRepository
import com.d104.pnt.data.repository.GameRoomRepositoryImpl
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.data.repository.LocationRepositoryImpl
import com.d104.pnt.data.repository.ProfileRepository
import com.d104.pnt.data.repository.ProfileRepositoryImpl
import com.d104.pnt.data.repository.WalkieRepository
import com.d104.pnt.data.repository.WalkieRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindWalkieRepository(
        impl: WalkieRepositoryImpl
    ): WalkieRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindGameRoomRepository(
        impl: GameRoomRepositoryImpl
    ): GameRoomRepository
}
