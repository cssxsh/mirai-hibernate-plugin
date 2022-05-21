package xyz.cssxsh.mirai.test

import kotlinx.coroutines.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import xyz.cssxsh.mirai.hibernate.*

object MiraiHibernatePluginTest : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.mirai-hibernate-text",
        name = "mirai-hibernate-test",
        version = "0.0.0"
    ) {
        dependsOn("xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin", false)
    }
) {

    override fun onEnable() {

        val factory = MiraiHibernateConfiguration(plugin = this).buildSessionFactory()
        val metadata = factory.openSession().use { session ->
            session.doReturningWork { connection -> connection.metaData }
        }

        println(metadata.driverName)

        globalEventChannel().subscribeMessages {
            startsWith("撤销") {
                delay(10_000)

                val source = MiraiHibernateRecorder[source].single().toMessageSource()

                /**
                 * 这里的原消息内容来自 [xyz.cssxsh.mirai.hibernate.entry.MessageRecord.code] 反序列化的结果
                 */
                println(source.originalMessage)

                source.recall()
            }
        }
    }
}