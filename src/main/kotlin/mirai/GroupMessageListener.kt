package org.laolittle.plugin.genshin.mirai

import io.ktor.client.request.*
import kotlinx.serialization.SerializationException
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.message.data.buildForwardMessage
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import org.jetbrains.skia.EncodedImageFormat
import org.laolittle.plugin.genshin.CactusBot
import org.laolittle.plugin.genshin.CactusConfig
import org.laolittle.plugin.genshin.CactusData
import org.laolittle.plugin.genshin.api.ApiAccessDeniedException
import org.laolittle.plugin.genshin.api.genshin.GenshinBBSApi
import org.laolittle.plugin.genshin.api.internal.client
import org.laolittle.plugin.genshin.database.getUserData
import org.laolittle.plugin.genshin.model.GachaSimulator
import org.laolittle.plugin.genshin.service.AbstractCactusService
import org.laolittle.plugin.genshin.util.requireCookie
import org.laolittle.plugin.toExternalResource

object GroupMessageListener : AbstractCactusService() {
    private val users = mutableSetOf<Long>()
    override suspend fun main() {
        globalEventChannel().subscribeGroupMessages {
            (startsWith("原神") or startsWith(CactusConfig.botName)) Listener@{ foo ->
                val result = Regex("""(人物|十连|单抽|查询|test)(.*)""").find(foo)?.groupValues
                when (result?.get(1)) {
                    "十连" -> {
                        if (!users.add(sender.id)) return@Listener
                        val entities = GachaSimulator.gachaCharacter(sender.id, 1, 10)
                        GachaSimulator.renderGachaImage(entities).toExternalResource(EncodedImageFormat.JPEG)
                            .use { ex ->
                                subject.sendMessage(ex.uploadAsImage(subject) + message.quote())
                            }
                        users.remove(sender.id)
                    }
                    "查询" -> {
                        val userData = if (CactusConfig.allowAnonymous) getUserData(sender.id)
                        else sender.requireCookie { return@Listener }

                        val cookies = userData.data.cookies.takeIf { it.isNotBlank() } ?: CactusData.cookie

                        val uid = result[2].replace(Regex("""[\s]+"""), "").toLongOrNull()
                        if (uid == null || uid < 100000100 || uid > 700000000) {
                            subject.sendMessage("请输入正确的uid")
                            return@Listener
                        }
                        val query = kotlin.runCatching {
                            GenshinBBSApi.getPlayerInfo(uid, cookies, userData.data.uuid)
                        }.getOrElse {
                            when (it) {
                                is SerializationException -> subject.sendMessage("请求失败！请检查uid是否正确")
                                is ApiAccessDeniedException -> subject.sendMessage("获取失败: ${it.message}")
                                else -> CactusBot.logger.error(it)
                            }
                            return@Listener
                        }

                        subject.sendMessage(buildForwardMessage {
                            query.avatars.forEach { cInfo ->
                                add(bot, buildMessageChain {
                                    add(
                                        client.get<ByteArray>(cInfo.imageUrl).toExternalResource()
                                            .use { subject.uploadImage(it) })
                                    add(
                                        """
                                        名称: ${cInfo.name}
                                        命座: ${cInfo.constellation}
                                    """.trimIndent()
                                    )
                                })
                            }
                        })
                    }

                    "test" -> {
                        val userData = getUserData(sender.id)

                        sender.requireCookie { return@Listener }

                        GenshinBBSApi.getDailyNote(userData.genshinUID, userData.data.cookies, userData.data.uuid)
                            .also {
                                subject.sendMessage(it.toString())
                            }
                    }
                }
            }

            "原神登录" {
                subject.sendMessage("请加好友私聊发送")
            }
        }

    }
}