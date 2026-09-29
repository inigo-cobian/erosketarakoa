package com.erosketarakoa.app.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules or cancels the daily bargain worker based on the user's settings. */
@Singleton
class BargainScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun schedule(minuteOfDay: Int) {
        val request = PeriodicWorkRequestBuilder<BargainWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis(minuteOfDay), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    /** Sync schedule to settings in one call. */
    fun apply(enabled: Boolean, minuteOfDay: Int) {
        if (enabled) schedule(minuteOfDay) else cancel()
    }

    companion object {
        const val WORK_NAME = "daily-bargains"

        /** Milliseconds from now until the next occurrence of [minuteOfDay] (minutes past midnight). */
        fun initialDelayMillis(minuteOfDay: Int, now: Calendar = Calendar.getInstance()): Long {
            val target = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
                set(Calendar.MINUTE, minuteOfDay % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}
