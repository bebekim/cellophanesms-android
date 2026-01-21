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
class IncrementalAnalysisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val analysisRepository: AnalysisRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "IncrementalAnalysisWorker"
        const val WORK_NAME = "incremental_analysis_work"
        const val KEY_BATCH_SIZE = "batch_size"
        const val KEY_MESSAGE_IDS = "message_ids"
        const val KEY_PROCESSED = "processed"
        const val DEFAULT_BATCH_SIZE = 50
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting incremental analysis")

        try {
            val batchSize = inputData.getInt(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)

            // Get the last analyzed message timestamp
            val lastTimestamp = analysisRepository.getLastAnalyzedMessageTimestamp() ?: 0L

            // Get pending messages (either new or re-queued)
            val pendingMessages = analysisRepository.getPendingMessages(batchSize)

            if (pendingMessages.isEmpty()) {
                Log.d(TAG, "No pending messages to analyze")
                return@withContext Result.success()
            }

            Log.d(TAG, "Found ${pendingMessages.size} pending messages to analyze")

            // Analyze batch
            val result = analysisRepository.analyzeBatch(pendingMessages)

            if (result.isSuccess) {
                val response = result.getOrNull()
                Log.d(TAG, "Incremental analysis completed. Processed ${response?.processed ?: 0} messages.")

                // Update last incremental analysis timestamp
                val maxTimestamp = pendingMessages.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
                analysisRepository.updateIncrementalAnalysisTimestamp(maxTimestamp)

                Result.success(
                    Data.Builder()
                        .putInt(KEY_PROCESSED, response?.processed ?: pendingMessages.size)
                        .build()
                )
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Incremental analysis failed: ${error?.message}")

                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure(
                        Data.Builder()
                            .putString("error", error?.message)
                            .build()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Incremental analysis failed", e)

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
