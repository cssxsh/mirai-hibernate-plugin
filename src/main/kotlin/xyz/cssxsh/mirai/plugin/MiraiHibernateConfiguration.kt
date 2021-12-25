package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.console.plugin.jvm.*
import org.hibernate.boot.registry.*
import org.hibernate.cfg.*
import org.hibernate.dialect.function.*
import org.hibernate.type.*
import java.net.*

class MiraiHibernateConfiguration(private val loader: MiraiHibernateLoader) :
    Configuration(
        BootstrapServiceRegistryBuilder().applyClassLoader(MiraiHibernatePlugin::class.java.classLoader).build()
    ) {
    constructor(plugin: JvmPlugin) : this(loader = MiraiHibernateLoader(plugin = plugin))

    init {
        if (loader.autoScan) {
            scan()
        }
        load()
    }

    /**
     * @see [javax.persistence.Entity]
     */
    private fun scan() {
        val path = loader.packageName.replace('.', '/')
        val resource = loader.classLoader.getResource(path)!!
        val jar = (resource.openConnection()!! as JarURLConnection).jarFile
        for (entry in jar.entries()) {
            if (entry.name.startsWith(path) && entry.name.endsWith(".class")) {
                val name = entry.name.removeSuffix(".class").replace('/', '.')
                val clazz = loader.classLoader.loadClass(name)
                if (clazz.isAnnotationPresent(javax.persistence.Entity::class.java)) {
                    println(clazz.name)
                    addAnnotatedClass(clazz)
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
                // 设置 rand 别名
                addSqlFunction("rand", NoArgSQLFunction("random", StandardBasicTypes.DOUBLE))
            }
            url.startsWith("jdbc:mysql") -> {
                //addAnnotatedClass(MySqlVariable::class.java)
            }
        }
    }
}