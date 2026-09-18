package com.erosketarakoa.app.di

import com.erosketarakoa.app.data.Clock
import com.erosketarakoa.app.data.SystemClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemClock): Clock
}
