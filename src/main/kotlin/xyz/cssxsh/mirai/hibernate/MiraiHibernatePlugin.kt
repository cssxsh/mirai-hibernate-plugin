package xyz.cssxsh.mirai.hibernate

import net.mamoe.mirai.console.extension.*
import net.mamoe.mirai.console.plugin.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.hibernate.*
import java.util.*

/**
 * mirai-hibernate-plugin 插件主类
 */
public object MiraiHibernatePlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin",
        name = "mirai-hibernate-plugin",
        version = "2.5.0",
    ) {
        author("cssxsh")

        dependsOn("xyz.cssxsh.mirai.plugin.mirai-administrator", ">= 1.1.0", true)
    }
) {

    override fun PluginComponentStorage.onLoad() {
        checkPlatform(folder = dataFolder)
        val classLoader = this@MiraiHibernatePlugin::class.java.classLoader
        ServiceLoader.load(java.sql.Driver::class.java, classLoader)
            .forEach { driver ->
                logger.info { "Driver: ${driver::class.java.name} Version ${driver.majorVersion}.${driver.minorVersion}" }
            }
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

        for (plugin in PluginManager.plugins) {
            if (plugin !is JvmPlugin) continue
            when (plugin.id) {
                "net.mamoe.mirai-api-http" -> {
                    logger.info { "如果要使用 mirai-hibernate-plugin 为 mirai-api-http 提供消息持久化, 请安装 https://github.com/cssxsh/mirai-hibernate-http " }
                }
                "com.github.yyuueexxiinngg.onebot" -> continue
                else -> continue
            }
        }

        MiraiHibernateRecorder.registerTo(globalEventChannel())
    }

    override fun onDisable() {
        MiraiHibernateRecorder.cancelAll()
        factory.close()
    }
}