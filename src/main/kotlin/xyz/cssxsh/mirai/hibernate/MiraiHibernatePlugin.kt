package xyz.cssxsh.mirai.hibernate

import kotlinx.coroutines.*
import net.mamoe.mirai.console.extension.*
import net.mamoe.mirai.console.plugin.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.hibernate.*
import java.util.*

@PublishedApi
internal object MiraiHibernatePlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin",
        name = "mirai-hibernate-plugin",
        version = "2.7.1",
    ) {
        author("cssxsh")

        dependsOn("xyz.cssxsh.mirai.plugin.mirai-administrator", ">= 1.1.0", true)
    }
) {

    override fun PluginComponentStorage.onLoad() {
        checkPlatform(folder = dataFolder)
        jvmPluginClasspath.runCatching {
            downloadAndAddToPath(
                classLoader = pluginSharedLibrariesClassLoader,
                dependencies = listOf(mssql())
            )
        }.onFailure { cause ->
            logger.warning({ "添加 MSSQL 驱动失败" }, cause)
        }
        ServiceLoader.load(java.sql.Driver::class.java, jvmPluginClasspath.pluginClassLoader)
            .forEach { driver ->
                logger.info { "Driver: ${driver::class.java.name} Version ${driver.majorVersion}.${driver.minorVersion}" }
            }
    }

    override fun onEnable() {

        val configuration = MiraiHibernateConfiguration(plugin = this)

        with(configuration) {
            val url = getProperty("hibernate.connection.url").orEmpty()
            if (url.startsWith("jdbc:sqlite")) {
                logger.error { "Sqlite 不支持并发, 将替换为 H2Database" }
                setProperty("hibernate.connection.url", url.replace("sqlite", "h2"))
                setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                setProperty("hibernate.hikari.minimumIdle", "10")
                setProperty("hibernate.hikari.maximumPoolSize", "10")
            }
        }

        try {
            factory = configuration.buildSessionFactory()
        } catch (exception: Exception) {
            if ("Unsupported database file version" in exception.message.orEmpty()) {
                val current = System.currentTimeMillis()
                resolveDataFile("hibernate.h2.mv.db")
                    .renameTo(resolveDataFile("backup.$current.h2.mv.db"))
                resolveDataFile("hibernate.h2.trace.db")
                    .renameTo(resolveDataFile("backup.$current.h2.trace.db"))
                throw RuntimeException("本地文件和数据库版本不匹配，已备份，请重新启动", exception)
            }
            throw exception
        }

        val metadata = factory.fromSession { it.getDatabaseMetaData() }

        logger.info { "Database ${metadata.url} by ${metadata.driverName}." }
        logger.info { "如果你想使用其他类型的数据库，请自行修改:\n ${configuration.loader.configuration.toPath().toUri()}" }

        val backup = resolveConfigFile("hibernate.backup.properties")
        if (backup.exists()) {
            logger.info("发现备份配置，开始载入备份")
            launch {
                val properties = Properties()
                backup.inputStream().use(properties::load)
                configuration.restore(properties)
            }
        }

        for (plugin in PluginManager.plugins) {
            if (plugin !is JvmPlugin) continue
            when (plugin.description.id) {
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
        MiraiHibernateRecorder.cancel()
        factory.close()
    }
}