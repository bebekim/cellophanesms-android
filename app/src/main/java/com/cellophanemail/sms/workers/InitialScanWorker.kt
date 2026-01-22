package com.cellophanemail.sms.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.cellophanemail.sms.data.repository.AnalysisRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@HiltWorker
class InitialScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val analysisRepository: AnalysisRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "InitialScanWorker"
        const val WORK_NAME = "initial_scan_work"
        const val KEY_BATCH_SIZE = "batch_size"
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
        const val KEY_PROCESSED = "processed"
        const val KEY_ERROR_TYPE = "error_type"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_RETRY_COUNT = "retry_count"
        const val DEFAULT_BATCH_SIZE = 100
        const val MAX_RETRIES = 5
        const val MAX_BATCH_RETRIES = 3

        // Error types for UI
        const val ERROR_NETWORK = "NETWORK"
        const val ERROR_BACKEND = "BACKEND"
        const val ERROR_AUTH = "AUTH"
        const val ERROR_UNKNOWN = "UNKNOWN"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting initial scan (attempt ${runAttemptCount + 1})")

        try {
            // Check if already completed
            if (analysisRepository.isInitialScanCompleted()) {
                Log.d(TAG, "Initial scan already completed, skipping")
                return@withContext Result.success()
            }

            val batchSize = inputData.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)

            // Get total message count
            val totalMessages = analysisRepository.getTotalMessageCount()
            if (totalMessages == 0) {
                Log.d(TAG, "No messages to scan")
                analysisRepository.completeInitialScan()
                return@withContext Result.success()
            }

            // Start the scan
            analysisRepository.startInitialScan(totalMessages)

            var processedCount = 0
            var hasMore = true
            var consecutiveFailures = 0

            while (hasMore && !isStopped) {
                // Get next batch of unanalyzed messages
                val messages = analysisRepository.getUnanalyzedMessages(batchSize)

                if (messages.isEmpty()) {
                    hasMore = false
                    continue
                }

                Log.d(TAG, "Processing batch of ${messages.size} messages")

                // Analyze batch with retry
                val result = analyzeBatchWithRetry(messages)

                if (result.isSuccess) {
                    processedCount += messages.size
                    consecutiveFailures = 0

                    // Update progress
                    analysisRepository.updateScanProgress(processedCount)

                    // Report progress to WorkManager
                    val progress = (processedCount.toFloat() / totalMessages * 100).toInt()
                    setProgress(
                        Data.Builder()
                            .putInt(KEY_PROGRESS, progress)
                            .putInt(KEY_PROCESSED, processedCount)
                            .putInt(KEY_TOTAL, totalMessages)
                            .build()
                    )

                    Log.d(TAG, "Progress: $processedCount / $totalMessages ($progress%)")
                } else {
                    consecutiveFailures++
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Batch analysis failed (consecutive: $consecutiveFailures): ${error?.message}")

                    // If too many consecutive failures, pause and retry later
                    if (consecutiveFailures >= MAX_BATCH_RETRIES) {
                        Log.w(TAG, "Too many consecutive failures, pausing scan")
                        return@withContext handleError(error, processedCount, totalMessages)
                    }
                }

                // Adaptive delay based on success/failure
                val delay = if (consecutiveFailures > 0) {
                    // Exponential backoff: 1s, 2s, 4s
                    (1000L * (1 shl (consecutiveFailures - 1))).coerceAtMost(4000L)
                } else {
                    100L
                }
                kotlinx.coroutines.delay(delay)
            }

            if (isStopped) {
                Log.d(TAG, "Worker was stopped, will retry")
                return@withContext Result.retry()
            }

            // Mark scan as complete
            analysisRepository.completeInitialScan()
            Log.d(TAG, "Initial scan completed. Processed $processedCount messages.")

            Result.success(
                Data.Builder()
                    .putInt(KEY_PROCESSED, processedCount)
                    .putInt(KEY_TOTAL, totalMessages)
                    .build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Initial scan failed", e)
            handleError(e, 0, 0)
        }
    }

    private suspend fun analyzeBatchWithRetry(
        messages: List<com.cellophanemail.sms.data.local.entity.MessageEntity>
    ): kotlin.Result<Unit> {
        var lastException: Exception? = null

        repeat(MAX_BATCH_RETRIES) { attempt ->
            val result = analysisRepository.analyzeBatch(messages)
            if (result.isSuccess) {
                return kotlin.Result.success(Unit)
            }

            lastException = result.exceptionOrNull() as? Exception

            // Check if error is retryable
            if (!isRetryableError(lastException)) {
                return kotlin.Result.failure(lastException ?: Exception("Analysis failed"))
            }

            // Exponential backoff between retries
            val delay = (500L * (1 shl attempt)).coerceAtMost(2000L)
            Log.d(TAG, "Batch retry ${attempt + 1}/$MAX_BATCH_RETRIES after ${delay}ms")
            kotlinx.coroutines.delay(delay)
        }

        return kotlin.Result.failure(lastException ?: Exception("Max retries exceeded"))
    }

    private fun isRetryableError(error: Throwable?): Boolean {
        return when (error) {
            is SocketTimeoutException -> true
            is UnknownHostException -> true
            is IOException -> true
            else -> {
                // Check for HTTP 5xx errors (server errors are retryable)
                error?.message?.contains("50") == true ||
                error?.message?.contains("timeout", ignoreCase = true) == true
            }
        }
    }

    private fun handleError(error: Throwable?, processedCount: Int, totalMessages: Int): Result {
        val errorType = when (error) {
            is SocketTimeoutException, is UnknownHostException -> ERROR_NETWORK
            is IOException -> ERROR_NETWORK
            else -> {
                when {
                    error?.message?.contains("401") == true -> ERROR_AUTH
                    error?.message?.contains("50") == true -> ERROR_BACKEND
                    else -> ERROR_UNKNOWN
                }
            }
        }

        val outputData = Data.Builder()
            .putString(KEY_ERROR_TYPE, errorType)
            .putString(KEY_ERROR_MESSAGE, error?.message ?: "Unknown error")
            .putInt(KEY_PROCESSED, processedCount)
            .putInt(KEY_TOTAL, totalMessages)
            .putInt(KEY_RETRY_COUNT, runAttemptCount)
            .build()

        // Retry for network/backend errors, fail for auth errors
        return if (runAttemptCount < MAX_RETRIES && errorType != ERROR_AUTH) {
            Log.d(TAG, "Scheduling retry (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
            Result.retry()
        } else {
            Log.e(TAG, "Max retries exceeded or non-retryable error")
            Result.failure(outputData)
        }
    }
}
