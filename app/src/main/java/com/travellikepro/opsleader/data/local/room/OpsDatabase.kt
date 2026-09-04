package com.travellikepro.opsleader.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.travellikepro.opsleader.data.local.room.entity.UserEntity

@Database(
    entities = [
        UserEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class OpsDatabase : RoomDatabase() {
    // abstract fun userDao(): UserDao
}
