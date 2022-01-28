package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.console.extension.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*

public object MiraiHibernatePlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin",
        name = "mirai-hibernate-plugin",
        version = "1.0.4",
    ) {
        author("cssxsh")

        dependsOn("net.mamoe.mirai.mirai-slf4j-bridge", true)
    }
) {
    /**
     * @see [com.mchange.v2.log.MLogClasses.SLF4J_CNAME]
     * @see [org.jboss.logging.LoggerProviders.LOGGING_PROVIDER_KEY]
     */
    override fun PluginComponentStorage.onLoad() {
        System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.slf4j.Slf4jMLog")
        System.setProperty("org.jboss.logging.provider", "slf4j")
    }

    override fun onEnable() {

        MiraiHibernateRecorder.registerTo(globalEventChannel())

        val meta = useSession { session -> session.doReturningWork { connection -> connection.metaData } }
        logger.info { "Database ${meta.url} by ${meta.driverName}." }
    }

    override fun onDisable() {
        MiraiHibernateRecorder.cancelAll()
    }
}