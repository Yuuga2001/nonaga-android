package jp.riverapp.hexlide.domain.service

import jp.riverapp.hexlide.data.model.GameSession
import jp.riverapp.hexlide.data.remote.HexlideApi
import jp.riverapp.hexlide.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

class GamePollingService @Inject constructor(
    private val api: HexlideApi,
) {
    private var pollingJob: Job? = null
    private var consecutiveFailures = 0

    fun startPolling(
        scope: CoroutineScope,
        gameId: String,
        onUpdate: (GameSession) -> Unit,
        onError: () -> Unit = {},
    ) {
        stopPolling()
        consecutiveFailures = 0
        pollingJob = scope.launch {
            while (isActive) {
                delay(Constants.Timing.POLLING_INTERVAL_MS)
                if (!isActive) break
                try {
                    val game = api.getGame(gameId)
                    consecutiveFailures = 0
                    onUpdate(game)
                } catch (_: Exception) {
                    consecutiveFailures++
                    if (consecutiveFailures >= Constants.Timing.MAX_CONSECUTIVE_FAILURES) {
                        onError()
                        break
                    }
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
