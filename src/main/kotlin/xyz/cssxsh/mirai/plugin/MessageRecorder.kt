package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.mirai.plugin.entry.*
import java.sql.*
import kotlin.coroutines.*

object MessageRecorder : SimpleListenerHost() {

    @EventHandler
    fun MessageEvent.record() {
        useSession { session ->
            session.transaction.begin()
            try {
                session.save(MessageRecord(source = source, message = message))
                session.transaction.commit()
            } catch (cause: Throwable) {
                session.transaction.rollback()
                throw cause
            }
        }
    }

    @EventHandler
    fun MessagePostSendEvent<*>.record() {
        if (isFailure) return
        useSession { session ->
            session.transaction.begin()
            try {
                session.save(MessageRecord(source = source!!, message = message))
                session.transaction.commit()
            } catch (cause: Throwable) {
                session.transaction.rollback()
                throw cause
            }
        }
    }

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (exception) {
            is SQLException -> {
                logger.warning({ "SQL" }, exception)
            }
            else -> {
                logger.warning(exception)
            }
        }
    }

    operator fun get(source: MessageSource): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from(MessageRecord::class.java)
                criteria.select(record)
                    .where(
                        equal(record.get<Long>("bot"), source.bot.id),
                        equal(record.get<Long>("fromId"), source.fromId),
                        equal(record.get<Long>("targetId"), source.targetId),
                        equal(record.get<Int>("time"), source.time)
                    )
            }.list()
        }
    }
}