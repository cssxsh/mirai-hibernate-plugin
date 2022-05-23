package xyz.cssxsh.mirai.hibernate

import net.mamoe.mirai.console.extension.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.hibernate.*
import xyz.cssxsh.mirai.hibernate.entry.*

public object MiraiHibernatePlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin",
        name = "mirai-hibernate-plugin",
        version = "2.2.1",
    ) {
        author("cssxsh")

        dependsOn("net.mamoe.mirai.mirai-slf4j-bridge", true)
        dependsOn("xyz.cssxsh.mirai.plugin.mirai-administrator", ">= 1.1.0", true)
    }
) {
    /**
     * @see [org.jboss.logging.LoggerProviders.LOGGING_PROVIDER_KEY]
     */
    override fun PluginComponentStorage.onLoad() {
        try {
            Class.forName("net.mamoe.mirai.logger.bridge.slf4j.MiraiLoggerSlf4jFactory")
            System.setProperty("org.jboss.logging.provider", "slf4j")
        } catch (_: ClassNotFoundException) {
            // logger.warning { "未安装 mirai-slf4j-bridge." }
        }
    }

    private val test by lazy {
        System.getProperty("xyz.cssxsh.mirai.hibernate.test").toBoolean()
    }

    override fun onEnable() {

        MiraiHibernateRecorder.registerTo(globalEventChannel())

        val configuration = MiraiHibernateConfiguration(plugin = this)

        if (configuration.properties["hibernate.connection.provider_class"] == "org.hibernate.connection.C3P0ConnectionProvider") {
            logger.warning { "发现使用 C3P0ConnectionProvider，将替换为 HikariCPConnectionProvider" }
            configuration.properties["hibernate.connection.provider_class"] =
                "org.hibernate.hikaricp.internal.HikariCPConnectionProvider"
        }

        factory = configuration.buildSessionFactory()

        val metadata = currentSession.getDatabaseMetaData()

        logger.info { "Database ${metadata.url} by ${metadata.driverName}." }
        if (metadata.url.startsWith("jdbc:sqlite")) {
            logger.warning { "正在使用 Sqlite 数据库记录聊天内容，Sqlite 不支持并发，更换为其他数据库" }
            logger.warning { "正在使用 Sqlite 数据库记录聊天内容，Sqlite 不支持并发，更换为其他数据库" }
            logger.warning { "正在使用 Sqlite 数据库记录聊天内容，Sqlite 不支持并发，更换为其他数据库" }
        }

        if (test) {
            logger.info { "开启表情包调试" }
            globalEventChannel().subscribeMessages {
                "表情包" reply {
                    FaceRecord.random().toMessageContent()
                }
                """[0-9a-f]{32}""".toRegex() matchingReply {
                    MiraiHibernateRecorder.face(md5 = it.value)?.toMessageContent()
                }
            }
        }
    }

    override fun onDisable() {
        MiraiHibernateRecorder.cancelAll()
        factory.close()
    }
}