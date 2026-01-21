package com.cellophanemail.sms.workers

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisWorkManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val PERIODIC_ANALYSIS_WORK = "periodic_analysis_work"
        private const val PERIODIC_INTERVAL_HOURS = 6L
    }

    /**
     * Starts the initial scan workflow.
     * This should be called after SMS permissions are granted.
     */
    fun startInitialScan(batchSize: Int = InitialScanWorker.DEFAULT_BATCH_SIZE) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putInt(InitialScanWorker.KEY_BATCH_SIZE, batchSize)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<InitialScanWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .addTag(InitialScanWorker.WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            InitialScanWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Triggers an immediate incremental analysis.
     * This should be called when new messages arrive.
     */
    fun triggerIncrementalAnalysis(batchSize: Int = IncrementalAnalysisWorker.DEFAULT_BATCH_SIZE) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putInt(IncrementalAnalysisWorker.KEY_BATCH_SIZE, batchSize)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<IncrementalAnalysisWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .addTag(IncrementalAnalysisWorker.WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            IncrementalAnalysisWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Sets up periodic background analysis.
     * This catches any messages that might have been missed.
     */
    fun setupPeriodicAnalysis(intervalHours: Long = PERIODIC_INTERVAL_HOURS) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<IncrementalAnalysisWorker>(
            intervalHours,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .addTag(PERIODIC_ANALYSIS_WORK)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_ANALYSIS_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancels periodic analysis.
     */
    fun cancelPeriodicAnalysis() {
        workManager.cancelUniqueWork(PERIODIC_ANALYSIS_WORK)
    }

    /**
     * Cancels the initial scan.
     */
    fun cancelInitialScan() {
        workManager.cancelUniqueWork(InitialScanWorker.WORK_NAME)
    }

    /**
     * Cancels all analysis work.
     */
    fun cancelAllAnalysisWork() {
        workManager.cancelUniqueWork(InitialScanWorker.WORK_NAME)
        workManager.cancelUniqueWork(IncrementalAnalysisWorker.WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_ANALYSIS_WORK)
    }

    /**
     * Observes the initial scan work status.
     */
    fun observeInitialScanStatus(): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosForUniqueWorkLiveData(InitialScanWorker.WORK_NAME)
    }

    /**
     * Observes the incremental analysis work status.
     */
    fun observeIncrementalAnalysisStatus(): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosForUniqueWorkLiveData(IncrementalAnalysisWorker.WORK_NAME)
    }

    /**
     * Gets current initial scan work info.
     */
    suspend fun getInitialScanWorkInfo(): List<WorkInfo> = withContext(Dispatchers.IO) {
        workManager.getWorkInfosForUniqueWork(InitialScanWorker.WORK_NAME).get()
    }

    /**
     * Checks if initial scan is currently running.
     */
    suspend fun isInitialScanRunning(): Boolean {
        val workInfos = getInitialScanWorkInfo()
        return workInfos.any { it.state == WorkInfo.State.RUNNING }
    }

    /**
     * Checks if initial scan is enqueued or running.
     */
    suspend fun isInitialScanActive(): Boolean {
        val workInfos = getInitialScanWorkInfo()
        return workInfos.any {
            it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
        }
    }
}
