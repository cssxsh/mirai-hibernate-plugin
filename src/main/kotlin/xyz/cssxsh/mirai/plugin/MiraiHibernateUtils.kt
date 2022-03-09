package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.code.*
import net.mamoe.mirai.message.data.*
import org.hibernate.*
import xyz.cssxsh.mirai.plugin.entry.*

internal val logger get() = MiraiHibernatePlugin.logger

public val JvmPlugin.factory: SessionFactory get() = MiraiSessionCache[this]

internal val currentSession: Session by lazy {
//    try {
//        MiraiHibernatePlugin.factory.currentSession
//    } catch (_: Throwable) {
//        MiraiHibernatePlugin.factory.openSession()
//    }
    MiraiHibernatePlugin.factory.openSession()
}

internal fun <R> useSession(lock: Any? = null, block: (session: Session) -> R): R {
    return if (lock == null) {
        MiraiHibernatePlugin.factory.openSession().use(block)
    } else {
        synchronized(lock) {
            MiraiHibernatePlugin.factory.openSession().use(block)
        }
    }
}

public fun List<MessageRecord>.toForwardMessage(context: Contact) {
    buildForwardMessage(context) {
        for (record in this@toForwardMessage) {
            record.fromId at record.time says MiraiCode.deserializeMiraiCode(record.code)
        }
    }
}

public fun Sequence<MessageRecord>.toForwardMessage(context: Contact) {
    buildForwardMessage(context) {
        for (record in this@toForwardMessage) {
            record.fromId at record.time says MiraiCode.deserializeMiraiCode(record.code, context)
        }
    }
}