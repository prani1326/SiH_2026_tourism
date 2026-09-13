package com.travellikepro.opsleader.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // App level singletons can be provided here
    // SessionManager is currently using @Inject constructor
}
