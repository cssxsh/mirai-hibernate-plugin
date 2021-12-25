package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.mirai.plugin.entry.*
import java.sql.*
import kotlin.coroutines.*

object MiraiHibernateRecorder : SimpleListenerHost() {

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

    @EventHandler
    fun MessageRecallEvent.record() {
        useSession { session ->
            session.transaction.begin()
            try {
                session.withCriteriaUpdate<MessageRecord> { criteria ->
                    val record = criteria.from(MessageRecord::class.java)
                    criteria.set(record.get("recall"), true)
                        .where(
                            equal(record.get<Long>("ids"), messageIds.joinToString()),
                            equal(record.get<Long>("internalIds"), messageInternalIds.joinToString()),
                            equal(record.get<Int>("time"), messageTime)
                        )
                }.executeUpdate()
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

    /**
     * 与 [source] 对应的记录
     * @see [MessageRecord.code]
     */
    operator fun get(source: MessageSource): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from(MessageRecord::class.java)
                criteria.select(record)
                    .where(
                        equal(record.get<Int>("kind"), source.kind),
                        equal(record.get<Long>("fromId"), source.fromId),
                        equal(record.get<Long>("targetId"), source.targetId),
                        equal(record.get<Int>("time"), source.time)
                    )
            }.list()
        }
    }

    /**
     * [bot] 发送的 或 [bot] 收到 的消息
     */
    operator fun get(bot: Bot, start: Int, end: Int): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from(MessageRecord::class.java)
                criteria.select(record)
                    .where(
                        between(record.get("time"), start, end),
                        equal(record.get<Long>("bot"), bot.id)
                    )
            }.list()
        }
    }

    /**
     * 发送到群 [group] 或 从 [group] 收到 消息
     */
    operator fun get(group: Group, start: Int, end: Int): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from(MessageRecord::class.java)
                criteria.select(record)
                    .where(
                        between(record.get("time"), start, end),
                        equal(record.get<Long>("kind"), MessageSourceKind.GROUP.ordinal),
                        equal(record.get<Long>("targetId"), group.id)
                    )
            }.list()
        }
    }

    /**
     * 发送到群 [friend] 或 从 [friend] 收到 的消息
     */
    operator fun get(friend: Friend, start: Int, end: Int): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from(MessageRecord::class.java)
                criteria.select(record)
                    .where(
                        between(record.get("time"), start, end),
                        equal(record.get<Long>("kind"), MessageSourceKind.FRIEND.ordinal),
                        or(
                            equal(record.get<Long>("fromId"), friend.id),
                            equal(record.get<Long>("targetId"), friend.id)
                        )
                    )
            }.list()
        }
    }

    /**
     * [member] 发送的消息
     */
    operator fun get(member: Member, start: Int, end: Int): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from(MessageRecord::class.java)
                criteria.select(record)
                    .where(
                        between(record.get("time"), start, end),
                        equal(record.get<Long>("kind"), MessageSourceKind.GROUP.ordinal),
                        equal(record.get<Long>("fromId"), member.id),
                        equal(record.get<Long>("targetId"), member.group.id)
                    )
            }.list()
        }
    }

    /**
     * 发送到群 [stranger] 或 从 [stranger] 收到 的消息
     */
    operator fun get(stranger: Stranger, start: Int, end: Int): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from(MessageRecord::class.java)
                criteria.select(record)
                    .where(
                        between(record.get("time"), start, end),
                        equal(record.get<Long>("kind"), MessageSourceKind.STRANGER.ordinal),
                        or(
                            equal(record.get<Long>("fromId"), stranger.id),
                            equal(record.get<Long>("targetId"), stranger.id)
                        )
                    )
            }.list()
        }
    }

}