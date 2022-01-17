package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.registerTo
import net.mamoe.mirai.utils.*

object MiraiHibernatePlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin",
        name = "mirai-hibernate-plugin",
        version = "1.0.2",
    ) {
        author("cssxsh")
    }
) {
    override fun onEnable() {

        MiraiHibernateRecorder.registerTo(globalEventChannel())

        val meta = useSession { session -> session.doReturningWork { connection -> connection.metaData } }
        logger.info { "Database ${meta.url} by ${meta.driverName}." }
    }

    override fun onDisable() {
        MiraiHibernateRecorder.cancelAll()
    }
}