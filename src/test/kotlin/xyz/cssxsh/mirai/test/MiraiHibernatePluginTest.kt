package xyz.cssxsh.mirai.test

import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import org.hibernate.*
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

    private lateinit var factory: SessionFactory

    override fun onEnable() {
        factory = MiraiHibernateConfiguration(plugin = this).buildSessionFactory()
        val metadata = factory.fromSession { session ->
            session.doReturningWork { connection -> connection.metaData }
        }

        println(metadata.driverName)

        // MiraiHibernateRecorder 的使用
        globalEventChannel().subscribeMessages {
            startsWith("record") {
                // 返回一个流，请记得关闭这个流
                MiraiHibernateRecorder[subject].use { steam ->
                    steam.forEach { record ->
                        // 转化成消息链
                        record.toMessageChain()

                        // 转化消息引用
                        // 这里的 originalMessage 来自 上面的 toMessageChain
                        record.toMessageSource().originalMessage
                    }
                }

                // 返回一个列表，第 2，3 参数是 开始时刻和结束时间
                MiraiHibernateRecorder[subject, 16000000, 160000001].forEach { record ->
                    // 转化成消息链
                    record.toMessageChain()
                    // 转化消息引用
                    // 这里的 originalMessage 来自 上面的 toMessageChain
                    record.toMessageSource().originalMessage
                }
            }
        }

        // MiraiH2 的使用
        val url = factory.fromSession { session ->
            MiraiH2.url(session = session)
        }

        println(url)
    }

    override fun onDisable() {
        factory.close()
    }
}