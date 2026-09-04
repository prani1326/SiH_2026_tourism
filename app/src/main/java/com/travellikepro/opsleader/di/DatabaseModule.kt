package com.travellikepro.opsleader.di

import android.content.Context
import androidx.room.Room
import com.travellikepro.opsleader.data.local.room.OpsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideOpsDatabase(@ApplicationContext context: Context): OpsDatabase {
        return Room.databaseBuilder(
            context,
            OpsDatabase::class.java,
            "ops_database"
        ).fallbackToDestructiveMigration()
            .build()
    }

    // @Provides
    // fun provideUserDao(database: OpsDatabase): UserDao {
    //     return database.userDao()
    // }
}
