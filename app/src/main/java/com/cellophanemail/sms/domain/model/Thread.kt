package com.cellophanemail.sms.domain.model

data class Thread(
    val threadId: String,
    val address: String,
    val contactName: String?,
    val contactPhotoUri: String?,
    val lastMessageTime: Long,
    val lastMessagePreview: String,
    val unreadCount: Int,
    val messageCount: Int,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val toxicityLevel: ToxicityClass
) {
    val displayName: String
        get() = contactName ?: address
}
