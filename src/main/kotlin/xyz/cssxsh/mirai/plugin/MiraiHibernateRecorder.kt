package xyz.cssxsh.mirai.plugin

import kotlinx.coroutines.*
import net.mamoe.mirai.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.hibernate.*
import xyz.cssxsh.mirai.plugin.entry.*
import java.sql.*
import kotlin.coroutines.*
import kotlin.streams.*

/**
 * 消息记录器 记录机器人发送、接受和撤销的消息
 * @see MessageRecord
 * @see MessageEvent
 * @see MessagePostSendEvent
 * @see MessageRecallEvent
 */
public object MiraiHibernateRecorder : SimpleListenerHost() {

    @EventHandler
    internal fun MessageEvent.record() {
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
    internal fun MessagePostSendEvent<*>.record() {
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
    internal fun MessageRecallEvent.record() {
        useSession { session ->
            session.transaction.begin()
            try {
                session.withCriteriaUpdate<MessageRecord> { criteria ->
                    val record = criteria.from()
                    criteria.set(record.get("recall"), true)
                        .where(
                            equal(record.get<String>("ids"), messageIds.joinToString()),
                            equal(record.get<String>("internalIds"), messageInternalIds.joinToString()),
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

    @EventHandler
    internal fun NudgeEvent.record() {
        useSession { session ->
            session.transaction.begin()
            try {
                session.save(NudgeRecord(event = this))
                session.transaction.commit()
            } catch (cause: Throwable) {
                session.transaction.rollback()
                throw cause
            }
        }
    }

    private fun Throwable.causes() = sequence {
        var cause = this@causes
        while (isActive) {
            yield(cause)
            cause = cause.cause ?: break
        }
    }

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (val cause = exception.causes().firstOrNull { it is SQLException } ?: exception) {
            is SQLException -> {
                logger.warning({ "SQLException" }, cause)
            }
            is CancellationException -> {
                // ignore ...
            }
            is ExceptionInEventHandlerException -> {
                logger.warning({ "Exception in Recorder" }, cause.cause)
            }
            else -> {
                logger.warning({ "Exception in Recorder" }, exception)
            }
        }
    }

    /**
     * 与 [source] 对应的记录
     * @see [MessageRecord.code]
     */
    public operator fun get(source: MessageSource): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from<MessageRecord>()
                criteria.select(record)
                    .where(
                        equal(record.get<Int>("kind"), source.kind.ordinal),
                        equal(record.get<Long>("fromId"), source.fromId),
                        equal(record.get<Long>("targetId"), source.targetId),
                        equal(record.get<Int>("time"), source.time)
                    )
            }.list()
        }
    }

    /**
     * [bot] 发送的 或 [bot] 收到 的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(bot: Bot, start: Int, end: Int): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from<MessageRecord>()
                criteria.select(record)
                    .where(
                        between(record.get("time"), start, end),
                        equal(record.get<Long>("bot"), bot.id)
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.list()
        }
    }

    /**
     * [bot] 发送的 或 [bot] 收到 的消息
     */
    public operator fun get(bot: Bot): Sequence<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from<MessageRecord>()
                criteria.select(record)
                    .where(equal(record.get<Long>("bot"), bot.id))
                    .orderBy(desc(record.get<Int>("time")))
            }.stream().asSequence()
        }
    }

    /**
     * 发送到群 [group] 或 从 [group] 收到 消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(group: Group, start: Int, end: Int): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from<MessageRecord>()
                criteria.select(record)
                    .where(
                        between(record.get("time"), start, end),
                        equal(record.get<Int>("kind"), MessageSourceKind.GROUP.ordinal),
                        equal(record.get<Long>("targetId"), group.id)
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.list()
        }
    }

    /**
     * 发送到群 [group] 或 从 [group] 收到 消息
     */
    public operator fun get(group: Group): Sequence<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from<MessageRecord>()
                criteria.select(record)
                    .where(
                        equal(record.get<Int>("kind"), MessageSourceKind.GROUP.ordinal),
                        equal(record.get<Long>("targetId"), group.id)
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.stream().asSequence()
        }
    }

    /**
     * 发送到群 [friend] 或 从 [friend] 收到 的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(friend: Friend, start: Int, end: Int): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from<MessageRecord>()
                criteria.select(record)
                    .where(
                        between(record.get("time"), start, end),
                        equal(record.get<Int>("kind"), MessageSourceKind.FRIEND.ordinal),
                        or(
                            equal(record.get<Long>("fromId"), friend.id),
                            equal(record.get<Long>("targetId"), friend.id)
                        )
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.list()
        }
    }

    /**
     * 发送到群 [friend] 或 从 [friend] 收到 的消息
     */
    public operator fun get(friend: Friend): Sequence<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from<MessageRecord>()
                criteria.select(record)
                    .where(
                        equal(record.get<Int>("kind"), MessageSourceKind.FRIEND.ordinal),
                        or(
                            equal(record.get<Long>("fromId"), friend.id),
                            equal(record.get<Long>("targetId"), friend.id)
                        )
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.stream().asSequence()
        }
    }

    /**
     * [member] 发送的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(member: Member, start: Int, end: Int): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from<MessageRecord>()
                criteria.select(record)
                    .where(
                        between(record.get("time"), start, end),
                        equal(record.get<Int>("kind"), MessageSourceKind.GROUP.ordinal),
                        equal(record.get<Long>("fromId"), member.id),
                        equal(record.get<Long>("targetId"), member.group.id)
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.list()
        }
    }

    /**
     * [member] 发送的消息
     */
    public operator fun get(member: Member): Sequence<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from<MessageRecord>()
                criteria.select(record)
                    .where(
                        equal(record.get<Int>("kind"), MessageSourceKind.GROUP.ordinal),
                        equal(record.get<Long>("fromId"), member.id),
                        equal(record.get<Long>("targetId"), member.group.id)
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.stream().asSequence()
        }
    }

    /**
     * 发送到群 [stranger] 或 从 [stranger] 收到 的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(stranger: Stranger, start: Int, end: Int): List<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from<MessageRecord>()
                criteria.select(record)
                    .where(
                        between(record.get("time"), start, end),
                        equal(record.get<Int>("kind"), MessageSourceKind.STRANGER.ordinal),
                        or(
                            equal(record.get<Long>("fromId"), stranger.id),
                            equal(record.get<Long>("targetId"), stranger.id)
                        )
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.list()
        }
    }

    /**
     * 发送到群 [stranger] 或 从 [stranger] 收到 的消息
     */
    public operator fun get(stranger: Stranger): Sequence<MessageRecord> {
        return useSession { session ->
            session.withCriteria<MessageRecord> { criteria ->
                val record = criteria.from<MessageRecord>()
                criteria.select(record)
                    .where(
                        equal(record.get<Int>("kind"), MessageSourceKind.STRANGER.ordinal),
                        or(
                            equal(record.get<Long>("fromId"), stranger.id),
                            equal(record.get<Long>("targetId"), stranger.id)
                        )
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.stream().asSequence()
        }
    }

    /**
     * 发送到群 [contact] 或 从 [contact] 收到 的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(contact: Contact, start: Int, end: Int): List<MessageRecord> {
        return when (contact) {
            is Bot -> get(bot = contact, start = start, end = end)
            is Group -> get(group = contact, start = start, end = end)
            is Friend -> get(friend = contact, start = start, end = end)
            is Member -> get(member = contact, start = start, end = end)
            is Stranger -> get(stranger = contact, start = start, end = end)
            else -> throw IllegalStateException("不支持查询的联系人 $contact")
        }
    }

    /**
     * 发送到群 [contact] 或 从 [contact] 收到 的消息
     */
    public operator fun get(contact: Contact): Sequence<MessageRecord> {
        return when (contact) {
            is Bot -> get(bot = contact)
            is Group -> get(group = contact)
            is Friend -> get(friend = contact)
            is Member -> get(member = contact)
            is Stranger -> get(stranger = contact)
            else -> throw IllegalStateException("不支持查询的联系人 $contact")
        }
    }
}