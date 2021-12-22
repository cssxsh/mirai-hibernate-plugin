package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.console.plugin.jvm.*
import org.hibernate.boot.registry.*
import org.hibernate.cfg.*
import org.hibernate.dialect.function.*
import org.hibernate.type.*
import java.io.*
import java.net.*
import java.sql.*

class MiraiHibernateConfiguration(private val plugin: JvmPlugin, annotated: Boolean = true) :
    Configuration(BootstrapServiceRegistryBuilder().applyClassLoader(MiraiHibernatePlugin::class.java.classLoader).build()) {

    private val default = """
                hibernate.connection.url=jdbc:sqlite:${plugin.dataFolder.resolve("hibernate.sqlite").toURI().toASCIIString()}
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

    private val classLoader: ClassLoader = plugin::class.java.classLoader

    private val packageName: String = plugin::class.java.packageName

    init {
        if (annotated) {
            scan()
        }
    }

    private fun scan() {
        val path = packageName.replace('.', '/')
        val resource = classLoader.getResource(path)!!
        val jar = (resource.openConnection()!! as JarURLConnection).jarFile
        for (entry in jar.entries()) {
            if (entry.name.startsWith(path) && entry.name.endsWith(".class")) {
                val clazz = classLoader.loadClass(entry.name.removeSuffix(".class").replace('/', '.'))
                if (clazz.isAnnotationPresent(javax.persistence.Entity::class.java)) {
                    println(clazz.name)
                    addAnnotatedClass(clazz)
                }
            }
        }
    }

    fun load(hibernate: File = plugin.configFolder.resolve("hibernate.properties")) {
        println(System.currentTimeMillis())
        hibernate.apply { if (exists().not()) writeText(default) }.reader().use(properties::load)
        val url = requireNotNull(getProperty("hibernate.connection.url")) { "hibernate.connection.url cannot is null" }
        when {
            url.startsWith("jdbc:sqlite") -> {
                // SQLite 是单文件数据库，最好只有一个连接
                setProperty("hibernate.c3p0.min_size", "${1}")
                setProperty("hibernate.c3p0.max_size", "${1}")
                // 设置 rand 别名
                addSqlFunction("rand", NoArgSQLFunction("random", StandardBasicTypes.DOUBLE))
            }
            url.startsWith("jdbc:mysql") -> {
                //addAnnotatedClass(MySqlVariable::class.java)
            }
        }
    }
}