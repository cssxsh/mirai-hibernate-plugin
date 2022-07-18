package xyz.cssxsh.mirai.hibernate

import net.mamoe.mirai.console.extension.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.hibernate.*

public object MiraiHibernatePlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin",
        name = "mirai-hibernate-plugin",
        version = "2.4.0",
    ) {
        author("cssxsh")

        dependsOn("net.mamoe.mirai.mirai-slf4j-bridge", true)
        dependsOn("xyz.cssxsh.mirai.plugin.mirai-administrator", ">= 1.1.0", true)
    }
) {

    override fun PluginComponentStorage.onLoad() {
        checkPlatform(folder = dataFolder)
    }

    override fun onEnable() {

        val configuration = MiraiHibernateConfiguration(plugin = this)

        with(configuration) {
            if (getProperty("hibernate.connection.provider_class") == "org.hibernate.connection.C3P0ConnectionProvider") {
                logger.warning { "发现使用 C3P0ConnectionProvider，将替换为 HikariCPConnectionProvider" }
                setProperty(
                    "hibernate.connection.provider_class",
                    "org.hibernate.hikaricp.internal.HikariCPConnectionProvider"
                )
            }
            if (getProperty("hibernate.dialect") == "org.sqlite.hibernate.dialect.SQLiteDialect") {
                logger.warning { "发现使用 org.sqlite.hibernate.dialect.SQLiteDialect，将替换为 org.hibernate.community.dialect.SQLiteDialect" }
                setProperty(
                    "hibernate.dialect",
                    "org.hibernate.community.dialect.SQLiteDialect"
                )
            }
        }

        factory = configuration.buildSessionFactory()

        val metadata = useSession { it.getDatabaseMetaData() }

        logger.info { "Database ${metadata.url} by ${metadata.driverName}." }
        if (metadata.url.startsWith("jdbc:sqlite")) {
            throw IllegalArgumentException("正在使用 Sqlite 数据库记录聊天内容，Sqlite 不支持并发，请更换为其他数据库")
        }

        MiraiHibernateRecorder.registerTo(globalEventChannel())
    }

    override fun onDisable() {
        MiraiHibernateRecorder.cancelAll()
        factory.close()
    }
}