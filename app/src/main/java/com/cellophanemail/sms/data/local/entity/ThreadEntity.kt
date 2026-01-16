package com.cellophanemail.sms.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "threads",
    indices = [
        Index(value = ["address"]),
        Index(value = ["last_message_time"])
    ]
)
data class ThreadEntity(
    @PrimaryKey
    @ColumnInfo(name = "thread_id")
    val threadId: String,

    val address: String,

    @ColumnInfo(name = "contact_name")
    val contactName: String?,

    @ColumnInfo(name = "contact_photo_uri")
    val contactPhotoUri: String?,

    @ColumnInfo(name = "last_message_id")
    val lastMessageId: String?,

    @ColumnInfo(name = "last_message_time")
    val lastMessageTime: Long,

    @ColumnInfo(name = "last_message_preview")
    val lastMessagePreview: String,

    @ColumnInfo(name = "unread_count")
    val unreadCount: Int = 0,

    @ColumnInfo(name = "message_count")
    val messageCount: Int = 0,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "is_muted")
    val isMuted: Boolean = false,

    @ColumnInfo(name = "toxicity_level")
    val toxicityLevel: String = "SAFE",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
