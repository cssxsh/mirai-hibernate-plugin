package xyz.cssxsh.mirai.plugin

import kotlinx.coroutines.*
import net.mamoe.mirai.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.internal.message.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.hibernate.*
import xyz.cssxsh.mirai.plugin.entry.*
import java.sql.*
import java.io.*
import kotlin.coroutines.*
import kotlin.streams.*

/**
 * 消息记录器 记录机器人发送、接受和撤销的消息
 * @see MessageRecord
 * @see MessageEvent
 * @see MessagePostSendEvent
 * @see MessageRecallEvent
 * @see NudgeEvent
 */
public object MiraiHibernateRecorder : SimpleListenerHost() {

    private fun <E : Serializable> E.record() {
        useSession { session ->
            session.transaction.begin()
            try {
                session.merge(this)
                session.transaction.commit()
            } catch (cause: Throwable) {
                session.transaction.rollback()
                throw cause
            }
        }
    }

    @EventHandler
    internal fun MessageEvent.record() {
        MessageRecord.fromSuccess(source = source, message = message).record()
    }

    @EventHandler
    internal fun MessagePostSendEvent<*>.record() {
        val source = source
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        val message = with(LightMessageRefiner) { message.dropMiraiInternalFlags() }
        if (source != null) {
            MessageRecord.fromSuccess(source = source, message = message).record()
        } else {
            MessageRecord.fromFailure(target = target, message = message).record()
        }
    }

    @EventHandler
    internal fun MessageRecallEvent.record() {
        for (record in get(this)) {
            record.copy(recall = true).record()
        }
    }

    @EventHandler
    internal fun NudgeEvent.record() {
        NudgeRecord(event = this).record()
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
     * 与 [event] 对应的记录
     * @see [MessageRecord.code]
     */
    public operator fun get(event: MessageRecallEvent): List<MessageRecord> {
        return currentSession.withCriteria<MessageRecord> { criteria ->
            val record = criteria.from<MessageRecord>()
            criteria.select(record)
                .where(
                    equal(
                        record.get<Int>("kind"), when (event) {
                            is MessageRecallEvent.FriendRecall -> MessageSourceKind.FRIEND.ordinal
                            is MessageRecallEvent.GroupRecall -> MessageSourceKind.GROUP.ordinal
                        }
                    ),
                    equal(record.get<String>("ids"), event.messageIds.joinToString()),
                    equal(record.get<String>("internalIds"), event.messageInternalIds.joinToString()),
                    equal(record.get<Int>("time"), event.messageTime)
                )
        }.list().takeUnless { it.isNullOrEmpty() } ?: currentSession.withCriteria<MessageRecord> { criteria ->
            val record = criteria.from<MessageRecord>()
            criteria.select(record)
                .where(
                    equal(
                        record.get<Int>("kind"), when (event) {
                            is MessageRecallEvent.FriendRecall -> MessageSourceKind.FRIEND.ordinal
                            is MessageRecallEvent.GroupRecall -> MessageSourceKind.GROUP.ordinal
                        }
                    ),
                    equal(record.get<Long>("fromId"), event.authorId),
                    equal(
                        record.get<Long>("targetId"), when (event) {
                            is MessageRecallEvent.FriendRecall -> event.operator.id
                            is MessageRecallEvent.GroupRecall -> event.group.id
                        }
                    ),
                    equal(record.get<Int>("time"), event.messageTime)
                )
        }.list()
    }

    /**
     * 与 [source] 对应的记录
     * @see [MessageRecord.code]
     */
    public operator fun get(source: MessageSource): List<MessageRecord> {
        return currentSession.withCriteria<MessageRecord> { criteria ->
            val record = criteria.from<MessageRecord>()
            criteria.select(record)
                .where(
                    equal(record.get<Int>("kind"), source.kind.ordinal),
                    equal(record.get<String>("ids"), source.ids.joinToString()),
                    equal(record.get<String>("internalIds"), source.internalIds.joinToString()),
                    equal(record.get<Int>("time"), source.time)
                )
        }.list().takeUnless { it.isNullOrEmpty() } ?: currentSession.withCriteria<MessageRecord> { criteria ->
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

    /**
     * [bot] 发送的 或 [bot] 收到 的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(bot: Bot, start: Int, end: Int): List<MessageRecord> {
        return currentSession.withCriteria<MessageRecord> { criteria ->
            val record = criteria.from<MessageRecord>()
            criteria.select(record)
                .where(
                    between(record.get("time"), start, end),
                    equal(record.get<Long>("bot"), bot.id)
                )
                .orderBy(desc(record.get<Int>("time")))
        }.list()
    }

    /**
     * [bot] 发送的 或 [bot] 收到 的消息
     */
    public operator fun get(bot: Bot): Sequence<MessageRecord> {
        return currentSession.withCriteria<MessageRecord> { criteria ->
            val record = criteria.from<MessageRecord>()
            criteria.select(record)
                .where(equal(record.get<Long>("bot"), bot.id))
                .orderBy(desc(record.get<Int>("time")))
        }.stream().asSequence()
    }

    /**
     * 发送到群 [group] 或 从 [group] 收到 消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(group: Group, start: Int, end: Int): List<MessageRecord> {
        return currentSession.withCriteria<MessageRecord> { criteria ->
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

    /**
     * 发送到群 [group] 或 从 [group] 收到 消息
     */
    public operator fun get(group: Group): Sequence<MessageRecord> {
        return currentSession.withCriteria<MessageRecord> { criteria ->
            val record = criteria.from<MessageRecord>()
            criteria.select(record)
                .where(
                    equal(record.get<Int>("kind"), MessageSourceKind.GROUP.ordinal),
                    equal(record.get<Long>("targetId"), group.id)
                )
                .orderBy(desc(record.get<Int>("time")))
        }.stream().asSequence()
    }

    /**
     * 发送到群 [friend] 或 从 [friend] 收到 的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(friend: Friend, start: Int, end: Int): List<MessageRecord> {
        return currentSession.withCriteria<MessageRecord> { criteria ->
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

    /**
     * 发送到群 [friend] 或 从 [friend] 收到 的消息
     */
    public operator fun get(friend: Friend): Sequence<MessageRecord> {
        return currentSession.withCriteria<MessageRecord> { criteria ->
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

    /**
     * [member] 发送的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(member: Member, start: Int, end: Int): List<MessageRecord> {
        return currentSession.withCriteria<MessageRecord> { criteria ->
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

    /**
     * [member] 发送的消息
     */
    public operator fun get(member: Member): Sequence<MessageRecord> {
        return currentSession.withCriteria<MessageRecord> { criteria ->
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

    /**
     * 发送到群 [stranger] 或 从 [stranger] 收到 的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(stranger: Stranger, start: Int, end: Int): List<MessageRecord> {
        return currentSession.withCriteria<MessageRecord> { criteria ->
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

    /**
     * 发送到群 [stranger] 或 从 [stranger] 收到 的消息
     */
    public operator fun get(stranger: Stranger): Sequence<MessageRecord> {
        return currentSession.withCriteria<MessageRecord> { criteria ->
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