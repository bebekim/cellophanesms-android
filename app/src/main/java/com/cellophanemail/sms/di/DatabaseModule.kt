package com.cellophanemail.sms.di

import android.content.Context
import androidx.room.Room
import com.cellophanemail.sms.data.local.dao.AnalysisStateDao
import com.cellophanemail.sms.data.local.dao.MessageDao
import com.cellophanemail.sms.data.local.dao.SenderSummaryDao
import com.cellophanemail.sms.data.local.dao.ThreadDao
import com.cellophanemail.sms.data.local.db.AppDatabase
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
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideThreadDao(database: AppDatabase): ThreadDao {
        return database.threadDao()
    }

    @Provides
    @Singleton
    fun provideSenderSummaryDao(database: AppDatabase): SenderSummaryDao {
        return database.senderSummaryDao()
    }

    @Provides
    @Singleton
    fun provideAnalysisStateDao(database: AppDatabase): AnalysisStateDao {
        return database.analysisStateDao()
    }
}
