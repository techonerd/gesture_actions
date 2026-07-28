package com.gestureactions.app.core.action

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry mapping Gesture IDs -> ActionHandlers with Mutex lock to prevent deadlocks.
 */
class ActionRegistry(private val context: Context) {

    private val mappings = ConcurrentHashMap<String, ActionHandler>()
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun registerMapping(gestureId: String, actionHandler: ActionHandler) {
        mappings[gestureId] = actionHandler
    }

    fun dispatchAction(gestureId: String) {
        val action = mappings[gestureId] ?: return
        scope.launch {
            mutex.withLock {
                action.execute(context)
            }
        }
    }
}
