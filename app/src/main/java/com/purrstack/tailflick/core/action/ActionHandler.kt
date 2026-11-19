package com.purrstack.tailflick.core.action

import android.content.Context

/**
 * Base interface for system actions executed when a gesture triggers.
 */
interface ActionHandler {
    val id: String
    val displayName: String
    fun execute(context: Context)
}
