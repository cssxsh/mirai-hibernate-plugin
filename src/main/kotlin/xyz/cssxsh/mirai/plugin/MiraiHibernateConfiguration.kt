package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.console.plugin.jvm.*
import org.hibernate.boot.registry.*
import org.hibernate.cfg.*
import xyz.cssxsh.hibernate.*
import java.io.*
import java.net.*
import java.sql.*

/**
 * 适用于插件的 Hibernate [Configuration]
 * @param loader 加载器，定义一些加载行为
 * @see [Configuration.addRandFunction]
 */
public class MiraiHibernateConfiguration(private val loader: MiraiHibernateLoader) :
    Configuration(
        BootstrapServiceRegistryBuilder()
            .applyClassLoader(loader.classLoader)
            .build()
    ) {
    public constructor(plugin: JvmPlugin) : this(loader = MiraiHibernateLoader(plugin = plugin))

    init {
        setProperty("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider")
        setProperty("hibernate.connection.isolation", "${Connection.TRANSACTION_READ_UNCOMMITTED}")
        load()
    }

    /**
     * @see [javax.persistence.Entity]
     */
    private fun scan() {
        val path = loader.packageName.replace('.', '/')
        val connection = loader.classLoader.getResource(path)?.openConnection() ?: throw IOException(path)
        when (connection) {
            is JarURLConnection -> {
                for (entry in connection.jarFile.entries()) {
                    if (entry.name.startsWith(path) && entry.name.endsWith(".class")) {
                        val name = entry.name.removeSuffix(".class").replace('/', '.')
                        val clazz = loader.classLoader.loadClass(name)
                        if (clazz.isAnnotationPresent(javax.persistence.Entity::class.java)) {
                            addAnnotatedClass(clazz)
                        }
                    }
                }
            }
            else -> {
                // XXX: FileURLConnection
                connection.inputStream.reader().forEachLine { filename ->
                    if (filename.endsWith(".class")) {
                        val name = filename.removeSuffix(".class")
                        val clazz = loader.classLoader.loadClass("${loader.packageName}.${name}")
                        if (clazz.isAnnotationPresent(javax.persistence.Entity::class.java)) {
                            addAnnotatedClass(clazz)
                        }
                    }
                }
            }
        }
    }

    private fun load() {
        if (loader.autoScan) scan()
        loader.configuration.apply { if (exists().not()) writeText(loader.default) }.reader().use(properties::load)
        val url = requireNotNull(getProperty("hibernate.connection.url")) { "hibernate.connection.url cannot is null" }
        when {
            url.startsWith("jdbc:sqlite") -> {
                // SQLite 是单文件数据库，最好只有一个连接
                setProperty("hibernate.c3p0.min_size", "${1}")
                setProperty("hibernate.c3p0.max_size", "${1}")
                setProperty("hibernate.c3p0.timeout", "${30000}")
                setProperty("hibernate.hikari.minimumIdle", "${1}")
                setProperty("hibernate.hikari.maximumPoolSize", "${1}")
                setProperty("hibernate.hikari.connectionTimeout", "${30000}")
                // 设置 rand 别名
                addRandFunction()
            }
            url.startsWith("jdbc:mysql") -> {
                //
            }
        }
    }
}