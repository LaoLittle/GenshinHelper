package org.laolittle.plugin.genshin.util

import org.laolittle.plugin.genshin.api.genshin.GenshinBBSApi
import org.laolittle.plugin.genshin.database.User

suspend fun User.signGenshin() =
    GenshinBBSApi.signGenshin(genshinUID, GenshinBBSApi.getServerFromUID(genshinUID), data.cookies, data.uuid)