package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.console.extension.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.hibernate.*

public object MiraiHibernatePlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin",
        name = "mirai-hibernate-plugin",
        version = "2.1.0",
    ) {
        author("cssxsh")

        dependsOn("net.mamoe.mirai.mirai-slf4j-bridge", true)
        dependsOn("xyz.cssxsh.mirai.plugin.mirai-administrator", true)
    }
) {
    /**
     * @see [com.mchange.v2.log.MLogClasses.SLF4J_CNAME]
     * @see [org.jboss.logging.LoggerProviders.LOGGING_PROVIDER_KEY]
     */
    override fun PluginComponentStorage.onLoad() {
        try {
            Class.forName("net.mamoe.mirai.logger.bridge.slf4j.MiraiLoggerSlf4jFactory")
            System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.slf4j.Slf4jMLog")
            System.setProperty("org.jboss.logging.provider", "slf4j")
        } catch (_: ClassNotFoundException) {
            logger.warning { "未安装 mirai-slf4j-bridge." }
        }
    }

    override fun onEnable() {

        MiraiHibernateRecorder.registerTo(globalEventChannel())

        val metadata = currentSession.getDatabaseMetaData()

        logger.info { "Database ${metadata.url} by ${metadata.driverName}." }
    }

    override fun onDisable() {
        MiraiHibernateRecorder.cancelAll()
        currentSession.close()
    }
}