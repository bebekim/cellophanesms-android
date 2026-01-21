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
        const val DEFAULT_BATCH_SIZE = 100
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting initial scan")

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

            while (hasMore && !isStopped) {
                // Get next batch of unanalyzed messages
                val messages = analysisRepository.getUnanalyzedMessages(batchSize)

                if (messages.isEmpty()) {
                    hasMore = false
                    continue
                }

                Log.d(TAG, "Processing batch of ${messages.size} messages")

                // Analyze batch
                val result = analysisRepository.analyzeBatch(messages)

                if (result.isSuccess) {
                    processedCount += messages.size

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
                    Log.e(TAG, "Batch analysis failed: ${result.exceptionOrNull()?.message}")
                    // Continue with next batch, don't fail the entire job
                }

                // Small delay to prevent overwhelming the backend
                kotlinx.coroutines.delay(100)
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

            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(
                    Data.Builder()
                        .putString("error", e.message)
                        .build()
                )
            }
        }
    }
}
