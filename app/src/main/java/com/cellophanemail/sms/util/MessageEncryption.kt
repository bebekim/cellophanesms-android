package com.cellophanemail.sms.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageEncryption @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "cellophane_message_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun encrypt(plaintext: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Prepend IV to encrypted data
        return iv + encrypted
    }

    fun decrypt(ciphertext: ByteArray): String {
        if (ciphertext.size < IV_LENGTH) {
            throw IllegalArgumentException("Ciphertext too short")
        }

        val iv = ciphertext.sliceArray(0 until IV_LENGTH)
        val encrypted = ciphertext.sliceArray(IV_LENGTH until ciphertext.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun encryptOrNull(plaintext: String?): ByteArray? {
        return plaintext?.let {
            try {
                encrypt(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun decryptOrNull(ciphertext: ByteArray?): String? {
        return ciphertext?.let {
            try {
                decrypt(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}
