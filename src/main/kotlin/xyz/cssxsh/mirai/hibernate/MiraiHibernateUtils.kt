package xyz.cssxsh.mirai.hibernate

import kotlinx.coroutines.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.*
import org.hibernate.*
import xyz.cssxsh.mirai.hibernate.entry.*

internal val logger get() = MiraiHibernatePlugin.logger

internal lateinit var factory: SessionFactory

internal val CoroutineScope.currentSession: Session
    get() {
        val session = factory.openSession()
        launch {
            delay(40_000)
            session.close()
        }
        return session
    }

internal fun <R> useSession(block: (session: Session) -> R): R {
    return factory.openSession().use { session ->
        val transaction = session.beginTransaction()
        try {
            val result = block.invoke(session)
            transaction.commit()
            result
        } catch (cause: Throwable) {
            transaction.rollback()
            throw cause
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