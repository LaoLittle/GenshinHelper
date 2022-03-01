package org.laolittle.plugin.genshin.service

const val aDay = 24 * 60 * 60 * 1000L

fun startServices() {
    setOf(GenshinSignProver, GenshinGachaCacheTimer).forEach(CactusService::start)
}