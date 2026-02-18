package com.cellophanemail.sms.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cellophanemail.sms.data.local.NerModelManager
import com.cellophanemail.sms.data.local.NerProviderPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

@HiltWorker
class NerModelDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val modelManager: NerModelManager,
    private val preferences: NerProviderPreferences
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL)
            ?: return Result.failure(workDataOf(KEY_ERROR to "No download URL provided"))

        val destFile = modelManager.modelPath
        val tempFile = File(destFile.parentFile, "${destFile.name}.tmp")

        return try {
            downloadFile(downloadUrl, tempFile)

            // Verify checksum if configured
            if (NerModelManager.MODEL_SHA256.isNotEmpty()) {
                val actualHash = sha256(tempFile)
                if (!actualHash.equals(NerModelManager.MODEL_SHA256, ignoreCase = true)) {
                    tempFile.delete()
                    return Result.failure(
                        workDataOf(KEY_ERROR to "Checksum mismatch: expected ${NerModelManager.MODEL_SHA256}, got $actualHash")
                    )
                }
            }

            // Atomic rename
            if (!tempFile.renameTo(destFile)) {
                tempFile.delete()
                return Result.failure(
                    workDataOf(KEY_ERROR to "Failed to move model to final location")
                )
            }
            preferences.setModelDownloaded(true)

            Log.i(TAG, "Model downloaded successfully: ${destFile.length()} bytes")
            Result.success()
        } catch (e: java.io.IOException) {
            Log.w(TAG, "Network error downloading model: ${e.message}")
            tempFile.delete()
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download model: ${e.message}")
            tempFile.delete()
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    private suspend fun downloadFile(urlString: String, destFile: File) {
        destFile.parentFile?.mkdirs()

        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 30_000
            connection.readTimeout = 120_000

            val totalBytes = connection.contentLengthLong.takeIf { it > 0 }
                ?: NerModelManager.MODEL_SIZE_BYTES
            var downloadedBytes = 0L

            connection.inputStream.buffered().use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // Report progress
                        val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                        setProgress(workDataOf(KEY_PROGRESS to progress))
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val TAG = "NerModelDownload"
        const val WORK_NAME = "ner_model_download"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"

        fun buildRequest(downloadUrl: String) = OneTimeWorkRequestBuilder<NerModelDownloadWorker>()
            .setInputData(Data.Builder().putString(KEY_DOWNLOAD_URL, downloadUrl).build())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .addTag(WORK_NAME)
            .build()
    }
}
