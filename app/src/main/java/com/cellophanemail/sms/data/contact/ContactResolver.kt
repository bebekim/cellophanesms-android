package com.cellophanemail.sms.data.contact

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ContactInfo(
    val displayName: String,
    val photoUri: String?
)

@Singleton
class ContactResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun lookupContact(phoneNumber: String): ContactInfo? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI
        )

        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val photoIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)

                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    val photo = if (photoIndex >= 0) cursor.getString(photoIndex) else null

                    name?.let { ContactInfo(it, photo) }
                } else {
                    null
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
            null
        } catch (e: Exception) {
            null
        }
    }
}
