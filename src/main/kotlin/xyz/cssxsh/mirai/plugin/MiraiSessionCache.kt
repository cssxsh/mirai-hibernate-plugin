package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.console.plugin.jvm.*
import org.hibernate.*
import java.util.*

internal object MiraiSessionCache : Map<JvmPlugin, SessionFactory> {

    private val cache = WeakHashMap<JvmPlugin, SessionFactory>()

    override val entries: Set<Map.Entry<JvmPlugin, SessionFactory>> get() = cache.entries

    override val keys: Set<JvmPlugin> get() = cache.keys

    override val size: Int get() = cache.size

    override val values: Collection<SessionFactory> get() = cache.values

    override fun containsKey(key: JvmPlugin): Boolean = cache.containsKey(key)

    override fun containsValue(value: SessionFactory): Boolean = cache.containsValue(value)

    override fun get(key: JvmPlugin): SessionFactory = synchronized(this) {
        cache.getOrPut(key) { MiraiHibernateConfiguration(plugin = key).buildSessionFactory() }
    }

    override fun isEmpty(): Boolean = cache.isEmpty()
}