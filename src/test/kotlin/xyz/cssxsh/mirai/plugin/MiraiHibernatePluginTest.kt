package xyz.cssxsh.mirai.plugin.xyz.cssxsh.mirai.plugin

import kotlinx.coroutines.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import xyz.cssxsh.mirai.plugin.*

/**
 * 也可以不注解 [MiraiHibernate]，这时 [loader] 会自动生成
 * @see [MiraiHibernateLoader]
 */
@MiraiHibernate(loader = MiraiHibernatePluginTest.Loader::class)
object MiraiHibernatePluginTest : KotlinPlugin(
    JvmPluginDescription(
        id = "mirai-hibernate-test",
        name = "mirai-hibernate-test",
        version = "0.0.0"
    ) {
        dependsOn("xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin", false)
    }
) {

    object Loader : MiraiHibernateLoader by MiraiHibernateLoader.Impl(plugin = this)

    override fun onEnable() {

        /**
         * @see JvmPlugin.factory
         * @see MiraiHibernateConfiguration
         */
        val metadata = factory.openSession().use { session ->
            session.doReturningWork { connection -> connection.metaData }
        }

        println(metadata.driverName)

        globalEventChannel().subscribeMessages {
            startsWith("撤销") {
                delay(10_000)

                val source = MiraiHibernateRecorder[source].single().toMessageSource()

                /**
                 * 这里的原消息内容来自 [xyz.cssxsh.mirai.plugin.entry.MessageRecord.code] 反序列化的结果
                 */
                println(source.originalMessage)

                source.recall()
            }
        }
    }
}