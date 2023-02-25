package xyz.cssxsh.mirai.hibernate

import jakarta.persistence.*
import kotlinx.coroutines.*
import net.mamoe.mirai.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.internal.message.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.hibernate.*
import xyz.cssxsh.mirai.hibernate.entry.*
import java.sql.*
import java.io.*
import java.util.*
import java.util.stream.*
import kotlin.coroutines.*

/**
 * 消息记录器 记录机器人发送、接受和撤销的消息
 * @see MessageRecord
 * @see MessageEvent
 * @see MessagePostSendEvent
 * @see MessageRecallEvent
 * @see NudgeEvent
 */
public object MiraiHibernateRecorder : SimpleListenerHost() {

    private fun <E : Serializable> E.merge(): E = factory.fromTransaction { session -> session.merge(this@merge) }

    @EventHandler(priority = EventPriority.HIGHEST)
    internal fun MessageEvent.record() {
        launch {
            val message = message.asSequence().filterNot { it is MessageSource }.toMessageChain()
            val source = if (this@record is MessageSyncEvent) {
                source.copyAmend {
                    fromId = bot.id
                    targetId = subject.id
                }
            } else {
                source
            }
            MessageRecord.fromSuccess(source = source, message = message).merge()
        }
        launch {
            for (item in message) {
                when {
                    item is Image && item.isEmoji -> FaceRecord.fromImage(image = item).merge()
                    item is MarketFace && item !is Dice -> FaceRecord.fromMarketFace(face = item).merge()
                }
            }
        }
    }

    @Suppress("INVISIBLE_MEMBER")
    @EventHandler(priority = EventPriority.HIGHEST)
    internal fun MessagePostSendEvent<*>.record() {
        launch {
            val source = source
            val message = with(LightMessageRefiner) { message.dropMiraiInternalFlags() }
            if (source != null) {
                MessageRecord.fromSuccess(source = source, message = message).merge()
            } else {
                MessageRecord.fromFailure(target = target, message = message).merge()
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    internal fun MessageRecallEvent.record() {
        val kind = when (this) {
            is MessageRecallEvent.FriendRecall -> {
                RecalledKind.SELF
            }
            is MessageRecallEvent.GroupRecall -> {
                if ((operator?.id ?: bot.id) != authorId) RecalledKind.ADMIN else RecalledKind.SELF
            }
        }
        launch {
            val targets = get(this@record)
            if (targets.isEmpty()) {
                logger.warning { "No found origin message for ${this@record}" }
            }
            for (record in targets) {
                record.copy(recalled = kind).merge()
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    internal fun NudgeEvent.record() {
        launch {
            NudgeRecord(event = this@record).merge()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    internal fun BotOnlineEvent.record() {
        launch {
            factory.fromTransaction { session ->
                session.merge(BotRecord.fromImpl(bot))
                for (friend in bot.friends) {
                    session.merge(FriendRecord.fromImpl(friend = friend))
                }
                for (group in bot.groups) {
                    session.merge(GroupMemberRecord.fromImpl(member = group.botAsMember))
                    for (member in group.members) {
                        session.merge(GroupMemberRecord.fromImpl(member = member))
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    internal fun BotOfflineEvent.record() {
        if (reconnect) return
        launch {
            BotRecord.fromImpl(bot).merge()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    internal fun BotNickChangedEvent.record() {
        launch {
            BotRecord.fromImpl(bot).merge()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    internal fun FriendEvent.record() {
        if (this is MessageEvent && ignore.add(friend.id).not()) return
        launch {
            FriendRecord.fromEvent(event = this@record).merge()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    internal fun GroupMemberEvent.record() {
        if (member !is NormalMember) return
        if (this is MessageEvent && ignore.add(member.id).not()) return
        launch {
            GroupMemberRecord.fromEvent(event = this@record).merge()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    internal fun GroupEvent.record() {
        if (this is MessageEvent && ignore.add(group.id).not()) return
        launch {
            GroupRecord.fromImpl(group = group).merge()
        }
    }

    private val ignore: MutableSet<Long> = Collections.newSetFromMap(WeakHashMap())

    private inline fun <reified T : Throwable> Throwable.unwrap(): T? {
        var current = this
        while (true) {
            if (current is T) return current
            current = current.cause ?: break
        }
        return null
    }

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (val cause = exception.unwrap<SQLException>() ?: exception.unwrap<PersistenceException>() ?: exception) {
            is SQLIntegrityConstraintViolationException ->
                logger.debug({ "SQLIntegrityConstraintViolationException in Recorder" }, cause)
            is SQLException -> {
                logger.warning({ "SQLException in Recorder" }, cause)
            }
            is PersistenceException -> {
                logger.warning({ "PersistenceException in Recorder" }, cause)
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
     * 通过 [md5] 获取表情包记录
     * @see FaceRecord.md5
     */
    public fun face(md5: String): FaceRecord? {
        return factory.fromSession { session ->
            session.get(FaceRecord::class.java, md5)
        }
    }

    /**
     * 与 [event] 对应的记录
     * @see [MessageRecord.code]
     */
    public operator fun get(event: MessageRecallEvent): List<MessageRecord> {
        return factory.fromSession { session ->
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(
                        equal(
                            record.get<MessageSourceKind>("kind"), when (event) {
                                is MessageRecallEvent.FriendRecall -> MessageSourceKind.FRIEND
                                is MessageRecallEvent.GroupRecall -> MessageSourceKind.GROUP
                            }
                        ),
                        equal(record.get<Long>("fromId"), event.authorId),
                        equal(
                            record.get<Long>("targetId"), when (event) {
                                is MessageRecallEvent.FriendRecall -> event.bot.id
                                is MessageRecallEvent.GroupRecall -> event.group.id
                            }
                        ),
                        equal(record.get<String>("ids"), event.messageIds.joinToString()),
                        equal(record.get<Int>("time"), event.messageTime)
                    )
                    .orderBy(asc(abs(diff(record.get("bot"), event.bot.id))))
            }.list()
        }
    }

    /**
     * 与 [source] 对应的记录
     * @see [MessageRecord.code]
     */
    public operator fun get(source: MessageSource): List<MessageRecord> {
        return factory.fromSession { session ->
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(
                        equal(record.get<MessageSourceKind>("kind"), source.kind),
                        equal(record.get<Long>("fromId"), source.fromId),
                        equal(record.get<Long>("targetId"), source.targetId),
                        equal(record.get<String>("ids"), source.ids.joinToString()),
                        equal(record.get<Int>("time"), source.time)
                    )
                    .orderBy(asc(abs(diff(record.get("bot"), source.botId))))
            }.list()
        }
    }

    /**
     * [bot] 发送的 或 [bot] 收到 的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(bot: Bot, start: Int, end: Int): List<MessageRecord> {
        return factory.fromSession { session ->
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
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
    public operator fun get(bot: Bot): Stream<MessageRecord> {
        val session = factory.openSession()
        return try {
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(equal(record.get<Long>("bot"), bot.id))
                    .orderBy(desc(record.get<Int>("time")))
            }.stream().onClose { session.close() }
        } catch (cause: Throwable) {
            session.close()
            throw cause
        }
    }

    /**
     * 发送到群 [group] 或 从 [group] 收到 消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(group: Group, start: Int, end: Int): List<MessageRecord> {
        return factory.fromSession { session ->
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(
                        equal(record.get<Int>("bot"), group.bot.id),
                        between(record.get("time"), start, end),
                        equal(record.get<MessageSourceKind>("kind"), MessageSourceKind.GROUP),
                        equal(record.get<Long>("targetId"), group.id)
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.list()
        }
    }

    /**
     * 发送到群 [group] 或 从 [group] 收到 消息
     */
    public operator fun get(group: Group): Stream<MessageRecord> {
        val session = factory.openSession()
        return try {
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(
                        equal(record.get<Int>("bot"), group.bot.id),
                        equal(record.get<MessageSourceKind>("kind"), MessageSourceKind.GROUP),
                        equal(record.get<Long>("targetId"), group.id)
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.stream().onClose { session.close() }
        } catch (cause: Throwable) {
            session.close()
            throw cause
        }
    }

    /**
     * 发送到群 [friend] 或 从 [friend] 收到 的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(friend: Friend, start: Int, end: Int): List<MessageRecord> {
        return factory.fromSession { session ->
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(
                        equal(record.get<Int>("bot"), friend.bot.id),
                        between(record.get("time"), start, end),
                        equal(record.get<MessageSourceKind>("kind"), MessageSourceKind.FRIEND),
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
    public operator fun get(friend: Friend): Stream<MessageRecord> {
        val session = factory.openSession()
        return try {
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(
                        equal(record.get<Int>("bot"), friend.bot.id),
                        equal(record.get<MessageSourceKind>("kind"), MessageSourceKind.FRIEND),
                        or(
                            equal(record.get<Long>("fromId"), friend.id),
                            equal(record.get<Long>("targetId"), friend.id)
                        )
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.stream().onClose { session.close() }
        } catch (cause: Throwable) {
            session.close()
            throw cause
        }
    }

    /**
     * [member] 发送的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(member: Member, start: Int, end: Int): List<MessageRecord> {
        return factory.fromSession { session ->
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(
                        equal(record.get<Int>("bot"), member.bot.id),
                        between(record.get("time"), start, end),
                        equal(record.get<MessageSourceKind>("kind"), MessageSourceKind.GROUP),
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
    public operator fun get(member: Member): Stream<MessageRecord> {
        val session = factory.openSession()
        return try {
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(
                        equal(record.get<Int>("bot"), member.bot.id),
                        equal(record.get<MessageSourceKind>("kind"), MessageSourceKind.GROUP),
                        equal(record.get<Long>("fromId"), member.id),
                        equal(record.get<Long>("targetId"), member.group.id)
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.stream().onClose { session.close() }
        } catch (cause: Throwable) {
            session.close()
            throw cause
        }
    }

    /**
     * 发送到群 [stranger] 或 从 [stranger] 收到 的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(stranger: Stranger, start: Int, end: Int): List<MessageRecord> {
        return factory.fromSession { session ->
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(
                        equal(record.get<Int>("bot"), stranger.bot.id),
                        between(record.get("time"), start, end),
                        equal(record.get<MessageSourceKind>("kind"), MessageSourceKind.STRANGER),
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
    public operator fun get(stranger: Stranger): Stream<MessageRecord> {
        val session = factory.openSession()
        return try {
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(
                        equal(record.get<Int>("bot"), stranger.bot.id),
                        equal(record.get<MessageSourceKind>("kind"), MessageSourceKind.STRANGER),
                        or(
                            equal(record.get<Long>("fromId"), stranger.id),
                            equal(record.get<Long>("targetId"), stranger.id)
                        )
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.stream().onClose { session.close() }
        } catch (cause: Throwable) {
            session.close()
            throw cause
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
    public operator fun get(contact: Contact): Stream<MessageRecord> {
        return when (contact) {
            is Bot -> get(bot = contact)
            is Group -> get(group = contact)
            is Friend -> get(friend = contact)
            is Member -> get(member = contact)
            is Stranger -> get(stranger = contact)
            else -> throw IllegalStateException("不支持查询的联系人 $contact")
        }
    }

    /**
     * 种类为 [kind] 的消息
     * @param start 开始时间
     * @param end 结束时间
     */
    public operator fun get(kind: MessageSourceKind, start: Int, end: Int): List<MessageRecord> {
        return factory.fromSession { session ->
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(
                        between(record.get("time"), start, end),
                        equal(record.get<MessageSourceKind>("kind"), kind)
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.list()
        }
    }

    /**
     * 种类为 [kind] 的消息
     */
    public operator fun get(kind: MessageSourceKind): Stream<MessageRecord> {
        val session = factory.openSession()
        return try {
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                query.select(record)
                    .where(
                        equal(record.get<MessageSourceKind>("kind"), kind)
                    )
                    .orderBy(desc(record.get<Int>("time")))
            }.stream().onClose { session.close() }
        } catch (cause: Throwable) {
            session.close()
            throw cause
        }
    }
}