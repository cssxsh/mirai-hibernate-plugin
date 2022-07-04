package xyz.cssxsh.mirai.hibernate

import kotlinx.coroutines.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import org.hibernate.*
import xyz.cssxsh.hibernate.*
import xyz.cssxsh.mirai.hibernate.entry.*

internal val logger by lazy {
    try {
        MiraiHibernatePlugin.logger
    } catch (_: Throwable) {
        MiraiLogger.Factory.create(MiraiHibernateRecorder::class)
    }
}

public fun checkPlatform() {
    // Termux
    if ("termux" in System.getProperty("user.dir")) {
        logger.info { "change platform to android-arm" }
        // sqlite base on native lib
        System.setProperty("org.sqlite.lib.path", "org/sqlite/native/Linux/android-arm")
    }
}

public lateinit var factory: SessionFactory

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

public fun FaceRecord.Companion.random(): FaceRecord {
    return useSession { session ->
        val count = session.withCriteria<Long> { criteria ->
            val record = criteria.from<FaceRecord>()
            criteria.select(count(record))
        }.uniqueResult().toInt()

        session.withCriteria<FaceRecord> { criteria ->
            val record = criteria.from<FaceRecord>()
            criteria.select(record)
                .orderBy(desc(record.get<String>("md5")))
        }.setFirstResult((0 until count).random()).setMaxResults(1).uniqueResult()
    }
}