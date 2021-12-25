package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.console.plugin.jvm.*
import java.io.*
import java.sql.*

/**
 * @since 2.0.1
 */
interface MiraiHibernateLoader {
    /**
     * 是否自动扫描已标记注解的类
     */
    val autoScan: Boolean

    /**
     * 自动扫描 的 起始包
     */
    val packageName: String

    /**
     * 自动扫描 的 类加载器
     */
    val classLoader: ClassLoader

    /**
     * 文件
     */
    val configuration: File

    /**
     * 默认配置
     */
    val default: String

    companion object {
        @JvmStatic
        operator fun invoke(plugin: JvmPlugin): MiraiHibernateLoader {
            return if (plugin is MiraiHibernateLoader) plugin else Impl(plugin = plugin)
        }
    }

    class Impl(
        override val autoScan: Boolean,
        override val packageName: String,
        override val classLoader: ClassLoader,
        override val configuration: File,
        override val default: String
    ) : MiraiHibernateLoader {
        constructor(plugin: JvmPlugin) : this(
            autoScan = true,
            packageName = plugin::class.java.packageName,
            classLoader = plugin::class.java.classLoader,
            configuration = plugin.configFolder.resolve("hibernate.properties"),
            default = """
                hibernate.connection.url=jdbc:sqlite:${
                plugin.dataFolder.resolve("hibernate.sqlite").toURI().toASCIIString()
            }
                hibernate.connection.driver_class=org.sqlite.JDBC
                hibernate.dialect=org.sqlite.hibernate.dialect.SQLiteDialect
                hibernate.connection.provider_class=org.hibernate.connection.C3P0ConnectionProvider
                hibernate.connection.isolation=${Connection.TRANSACTION_READ_UNCOMMITTED}
                hibernate.hbm2ddl.auto=update
                hibernate-connection-autocommit=${true}
                hibernate.connection.show_sql=${false}
                hibernate.autoReconnect=${true}
                hibernate.current_session_context_class=thread
            """.trimIndent()
        )
    }
}