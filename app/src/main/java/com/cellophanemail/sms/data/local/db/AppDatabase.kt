package com.cellophanemail.sms.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cellophanemail.sms.data.local.dao.MessageDao
import com.cellophanemail.sms.data.local.dao.ThreadDao
import com.cellophanemail.sms.data.local.entity.MessageEntity
import com.cellophanemail.sms.data.local.entity.ThreadEntity

@Database(
    entities = [
        MessageEntity::class,
        ThreadEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun threadDao(): ThreadDao

    companion object {
        const val DATABASE_NAME = "cellophane_sms_db"
    }
}
