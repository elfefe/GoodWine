package com.elfefe.goodwine.oltp

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.elfefe.goodwine.BaseApplication
import com.elfefe.goodwine.oltp.dao.BottleDao
import com.elfefe.goodwine.oltp.entities.Bottle

@Database(
    entities = [
        Bottle::class
    ],
    version = 1
)
abstract class OltpDatabase : RoomDatabase() {
    abstract fun bottleDao(): BottleDao

    companion object {
        val instance: OltpDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            Room.databaseBuilder(
                BaseApplication.instance,
                OltpDatabase::class.java,
                "oltp.db"
            ).build()
        }
    }
}