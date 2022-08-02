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
        version = "2.4.2",
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
            if (getProperty("hibernate.connection.url").orEmpty().startsWith("jdbc:sqlite")) {
                logger.error { "Sqlite 不支持并发, 将替换为 H2Database" }
                setProperty("hibernate.connection.url", getProperty("hibernate.connection.url").replace("sqlite", "h2"))
                setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                setProperty("hibernate.hikari.minimumIdle", "10")
                setProperty("hibernate.hikari.maximumPoolSize", "10")
            }
        }

        factory = configuration.buildSessionFactory()

        val metadata = factory.fromSession { it.getDatabaseMetaData() }

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