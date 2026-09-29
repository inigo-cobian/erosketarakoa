package com.erosketarakoa.app.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.erosketarakoa.app.data.ShoppingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs the pure bargain engine over cached prices and posts a summary notification.
 * Reuses [ShoppingRepository.computeBargains] — the same engine as the Today's bargains screen.
 */
@HiltWorker
class BargainWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: ShoppingRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Syncs live prices into the cache first; falls back to the cache when offline.
        val flagged = repository.syncAndComputeBargains()
        BargainSummary.build(flagged)?.let { BargainNotifier.show(applicationContext, it) }
        return Result.success()
    }
}
