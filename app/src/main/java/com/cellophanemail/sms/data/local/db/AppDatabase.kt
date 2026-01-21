package com.cellophanemail.sms.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cellophanemail.sms.data.local.dao.AnalysisStateDao
import com.cellophanemail.sms.data.local.dao.MessageDao
import com.cellophanemail.sms.data.local.dao.SenderSummaryDao
import com.cellophanemail.sms.data.local.dao.ThreadDao
import com.cellophanemail.sms.data.local.entity.AnalysisStateEntity
import com.cellophanemail.sms.data.local.entity.MessageEntity
import com.cellophanemail.sms.data.local.entity.SenderSummaryEntity
import com.cellophanemail.sms.data.local.entity.ThreadEntity

@Database(
    entities = [
        MessageEntity::class,
        ThreadEntity::class,
        SenderSummaryEntity::class,
        AnalysisStateEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun threadDao(): ThreadDao
    abstract fun senderSummaryDao(): SenderSummaryDao
    abstract fun analysisStateDao(): AnalysisStateDao

    companion object {
        const val DATABASE_NAME = "cellophane_sms_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create sender_summaries table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sender_summaries (
                        sender_id TEXT PRIMARY KEY NOT NULL,
                        contact_name TEXT,
                        contact_photo_uri TEXT,
                        total_message_count INTEGER NOT NULL DEFAULT 0,
                        filtered_count INTEGER NOT NULL DEFAULT 0,
                        noise_count INTEGER NOT NULL DEFAULT 0,
                        toxic_logistics_count INTEGER NOT NULL DEFAULT 0,
                        criticism_count INTEGER NOT NULL DEFAULT 0,
                        contempt_count INTEGER NOT NULL DEFAULT 0,
                        defensiveness_count INTEGER NOT NULL DEFAULT 0,
                        stonewalling_count INTEGER NOT NULL DEFAULT 0,
                        total_toxicity_score REAL NOT NULL DEFAULT 0,
                        last_message_timestamp INTEGER,
                        last_filtered_timestamp INTEGER,
                        engine_version TEXT,
                        created_at INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sender_summaries_filtered_count ON sender_summaries(filtered_count)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sender_summaries_last_filtered_timestamp ON sender_summaries(last_filtered_timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sender_summaries_total_toxicity_score ON sender_summaries(total_toxicity_score)")

                // Create analysis_state table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS analysis_state (
                        id TEXT PRIMARY KEY NOT NULL,
                        initial_scan_completed INTEGER NOT NULL DEFAULT 0,
                        initial_scan_started_at INTEGER,
                        initial_scan_completed_at INTEGER,
                        total_messages_to_scan INTEGER NOT NULL DEFAULT 0,
                        messages_scanned INTEGER NOT NULL DEFAULT 0,
                        last_incremental_analysis_at INTEGER,
                        last_analyzed_message_timestamp INTEGER,
                        current_job_id TEXT,
                        engine_version TEXT NOT NULL DEFAULT 'v1',
                        dashboard_toxic_senders INTEGER NOT NULL DEFAULT 0,
                        dashboard_filtered_messages INTEGER NOT NULL DEFAULT 0,
                        dashboard_noise_messages INTEGER NOT NULL DEFAULT 0,
                        dashboard_toxic_logistics_messages INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Add new columns to messages table for batch analysis
                db.execSQL("ALTER TABLE messages ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN severity TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN has_logistics INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN engine_version TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN analyzed_at INTEGER")
            }
        }
    }
}
