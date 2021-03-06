package xyz.cssxsh.mirai.hibernate

import net.mamoe.mirai.console.plugin.jvm.*
import java.io.*
import java.sql.*
import kotlin.io.path.*

public interface MiraiHibernateLoader {
    /**
     * 是否自动扫描已标记注解的类
     */
    public val autoScan: Boolean

    /**
     * 自动扫描 的 起始包
     */
    public val packageName: String

    /**
     * 自动扫描 的 类加载器
     */
    public val classLoader: ClassLoader

    /**
     * 文件
     */
    public val configuration: File

    /**
     * 默认配置
     */
    public val default: String

    public companion object {
        @JvmStatic
        public operator fun invoke(plugin: JvmPlugin): MiraiHibernateLoader = Impl(plugin = plugin)

        private fun JvmPlugin.database(filename: String): String {
            return try {
                Path(".")
                    .toAbsolutePath()
                    .relativize(resolveDataPath(filename))
                    .joinToString(separator = "/", prefix = "./")
            } catch (_: Throwable) {
                filename
            }
        }
    }

    public data class Impl(
        override val autoScan: Boolean,
        override val packageName: String,
        override val classLoader: ClassLoader,
        override val configuration: File,
        override val default: String
    ) : MiraiHibernateLoader {
        public constructor(plugin: JvmPlugin) : this(
            autoScan = true,
            packageName = with(plugin::class.java) {
                val packagePath = packageName.replace('.', '/')
                for(name in listOf("entry", "entity", "entities", "model", "models", "bean", "beans", "dto")) {
                    classLoader.getResource("$packagePath/$name") ?: continue
                    return@with "$packageName.$name"
                }
                packageName
            },
            classLoader = plugin::class.java.classLoader,
            configuration = plugin.configFolder.resolve("hibernate.properties"),
            default = """
                hibernate.connection.url=jdbc:h2:file:${plugin.database("hibernate.h2")}
                hibernate.connection.driver_class=org.h2.Driver
                hibernate.dialect=org.hibernate.dialect.H2Dialect
                hibernate.connection.provider_class=org.hibernate.hikaricp.internal.HikariCPConnectionProvider
                hibernate.hikari.connectionTimeout=180000
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