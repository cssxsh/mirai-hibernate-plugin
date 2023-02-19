package xyz.cssxsh.mirai.hibernate

import jakarta.persistence.*
import net.mamoe.mirai.console.plugin.jvm.*
import org.hibernate.*
import org.hibernate.boot.registry.*
import org.hibernate.cfg.*
import xyz.cssxsh.hibernate.*
import java.sql.*
import java.util.*
import kotlin.streams.*

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
        // 载入文件
        loader.configuration.apply { if (exists().not()) writeText(loader.default) }.inputStream().use(properties::load)
        if (loader.autoScan) scan(packageName = loader.packageName)
        default()
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
        val reflections = org.reflections.Reflections(
            org.reflections.util.ConfigurationBuilder()
                .forPackage(packageName, loader.classLoader)
        )
        val query = org.reflections.scanners.Scanners.TypesAnnotated
            .of(Entity::class.java, Embeddable::class.java, MappedSuperclass::class.java)
            .asClass<java.io.Serializable>(loader.classLoader)
        query.apply(reflections.store).forEach { clazz ->
            addAnnotatedClass(clazz)
        }
    }

    private fun setPropertyIfAbsent(propertyName: String, value: String) {
        if (propertyName !in properties) {
            properties.setProperty(propertyName, value)
        }
    }

    /**
     * @see org.hibernate.dialect.MySQLDialect
     * @see org.hibernate.dialect.MariaDBDialect
     * @see org.hibernate.dialect.H2Dialect
     * @see org.hibernate.dialect.PostgreSQLDialect
     * @see org.hibernate.dialect.SQLServerDialect
     * @see org.hibernate.dialect.OracleDialect
     * @see org.hibernate.community.dialect.SQLiteDialect
     */
    private fun Configuration.default() {
        // 设置默认数据库连接池
        setPropertyIfAbsent("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider")
        // 设置默认事务隔离级别
        setPropertyIfAbsent("hibernate.connection.isolation", "${Connection.TRANSACTION_READ_UNCOMMITTED}")
        // 设置 rand 别名
        addRandFunction()
        // 设置 dice 宏
        addDiceFunction()
        val url = getProperty("hibernate.connection.url") ?: throw NoSuchElementException("jdbc url no found!")
        when {
            url.startsWith("jdbc:h2") -> {
                setPropertyIfAbsent("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
            }
            url.startsWith("jdbc:sqlite") -> {
                // SQLite 是单文件数据库，最好只有一个连接
                setPropertyIfAbsent("hibernate.hikari.minimumIdle", "1")
                setPropertyIfAbsent("hibernate.hikari.maximumPoolSize", "1")
                setPropertyIfAbsent("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect")
            }
            url.startsWith("jdbc:mysql") -> {
                setPropertyIfAbsent("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect")
            }
            url.startsWith("jdbc:postgresql") -> {
                setPropertyIfAbsent("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            }
            url.startsWith("jdbc:sqlserver") -> {
                setPropertyIfAbsent("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect")
                setPropertyIfAbsent("hibernate.globally_quoted_identifiers", "true")
            }
            url.startsWith("jdbc:oracle") -> {
                setPropertyIfAbsent("hibernate.dialect", "org.hibernate.dialect.OracleDialect")
            }
        }
    }

    /**
     * 数据库还原
     * @since 2.7.0
     */
    public fun restore(properties: Properties) {
        val another = another(properties = properties)

        buildSessionFactory().use { target ->
            val entities = target.metamodel.entities
            another.buildSessionFactory().use { source ->
                for (entity in entities) {
                    transfer(source, target, entity.javaType, 4096)
                }
            }
        }
    }

    /**
     * 数据库备份
     * @since 2.7.0
     */
    public fun backup(properties: Properties) {
        val another = another(properties = properties)

        buildSessionFactory().use { source ->
            val entities = source.metamodel.entities
            another.buildSessionFactory().use { target ->
                for (entity in entities) {
                    transfer(source, target, entity.javaType, 4096)
                }
            }
        }
    }

    @PublishedApi
    internal fun another(properties: Properties): Configuration {
        require(properties.getProperty("hibernate.connection.url") != getProperty("hibernate.connection.url")) {
            "Both database url are the same!"
        }
        val another = Configuration(
            BootstrapServiceRegistryBuilder()
                .applyClassLoader(loader.classLoader)
                .build()
        )
        another.addProperties(properties)
        another.default()
        buildSessionFactory().use { target ->
            for (managedType in target.metamodel.managedTypes) {
                another.addAnnotatedClass(managedType.javaType)
            }
        }
        return another
    }

    @PublishedApi
    internal fun transfer(source: SessionFactory, target: SessionFactory, entity: Class<*>, chunked: Int) {
        source.fromSession { session ->
            val query = session.criteriaBuilder.createQuery(entity)
            query.from(entity)
            session.createQuery(query).stream()
                .asSequence().chunked(chunked)
                .forEach { list ->
                    target.fromTransaction { list.forEach(it::merge) }
                }
        }
    }
}