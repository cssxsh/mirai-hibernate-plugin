package xyz.cssxsh.mirai.hibernate

import jakarta.persistence.*
import net.mamoe.mirai.console.plugin.jvm.*
import org.hibernate.boot.registry.*
import org.hibernate.cfg.*
import xyz.cssxsh.hibernate.*
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
     * 扫描指定包名下的 实体类 (被 Entity, Embeddable, MappedSuperclass 标记的类）
     * @see MiraiHibernateLoader.autoScan
     * @see MiraiHibernateLoader.packageName
     * @see jakarta.persistence.Entity
     * @see jakarta.persistence.Embeddable
     * @see jakarta.persistence.MappedSuperclass
     */
    public fun scan(packageName: String) {
        val reflections = org.reflections.Reflections(packageName)
        val query = org.reflections.scanners.Scanners.TypesAnnotated
            .of(Entity::class.java, Embeddable::class.java, MappedSuperclass::class.java)
            .asClass<java.io.Serializable>(loader.classLoader)
        query.apply(reflections.store).forEach { clazz ->
            addAnnotatedClass(clazz)
        }
    }

    /**
     * @see org.hibernate.dialect.MySQLDialect
     * @see org.hibernate.dialect.MariaDBDialect
     * @see org.hibernate.dialect.H2Dialect
     * @see org.hibernate.dialect.PostgreSQLDialect
     * @see org.hibernate.community.dialect.SQLiteDialect
     */
    private fun load() {
        if (loader.autoScan) scan(packageName = loader.packageName)
        loader.configuration.apply { if (exists().not()) writeText(loader.default) }.inputStream().use(properties::load)
        // 设置 rand 别名
        addRandFunction()
        // 设置 dice 宏
        addDiceFunction()
        val url = getProperty("hibernate.connection.url").orEmpty()
        when {
            url.startsWith("jdbc:h2") -> {
                //
            }
            url.startsWith("jdbc:sqlite") -> {
                // SQLite 是单文件数据库，最好只有一个连接
                setProperty("hibernate.hikari.minimumIdle", "1")
                setProperty("hibernate.hikari.maximumPoolSize", "1")
            }
            url.startsWith("jdbc:mysql") -> {
                //
            }
            url.startsWith("jdbc:postgresql") -> {
                //
            }
        }
    }
}