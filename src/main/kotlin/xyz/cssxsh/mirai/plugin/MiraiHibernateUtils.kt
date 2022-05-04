package xyz.cssxsh.mirai.plugin

import kotlinx.coroutines.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.*
import org.hibernate.*
import xyz.cssxsh.mirai.plugin.entry.*

internal val logger get() = MiraiHibernatePlugin.logger

internal lateinit var factory: SessionFactory

internal val CoroutineScope.currentSession: Session
    get() {
        val session = factory.openSession()
        launch {
            delay(1000)
            session.close()
        }
        return session
    }

internal fun <R> useSession(lock: Any? = null, block: (session: Session) -> R): R {
    return if (lock == null) {
        factory.openSession().use(block)
    } else {
        synchronized(lock) {
            factory.openSession().use(block)
        }
    }
}

public fun List<MessageRecord>.toForwardMessage(context: Contact) {
    buildForwardMessage(context) {
        for (record in this@toForwardMessage) {
            record.fromId at record.time says record.toMessageChain()
        }
    }
}

public fun Sequence<MessageRecord>.toForwardMessage(context: Contact) {
    buildForwardMessage(context) {
        for (record in this@toForwardMessage) {
            record.fromId at record.time says record.toMessageChain()
        }
    }
}