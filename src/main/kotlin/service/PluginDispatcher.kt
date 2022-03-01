package org.laolittle.plugin.genshin.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.laolittle.plugin.genshin.CactusBot
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object PluginDispatcher {
    private val dispatcherContext = EmptyCoroutineContext

    private val coroutineContext: CoroutineContext get() = SupervisorJob(CactusBot.coroutineContext.job) + dispatcherContext

    fun <T> runBlocking(block: suspend CoroutineScope.() -> T): T =
        runBlocking(coroutineContext, block)

}