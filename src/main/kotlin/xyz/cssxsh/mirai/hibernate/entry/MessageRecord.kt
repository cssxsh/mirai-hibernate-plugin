package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*
import net.mamoe.mirai.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.code.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import xyz.cssxsh.hibernate.*
import xyz.cssxsh.mirai.hibernate.*

/**
 * 戳一戳记录
 * @param id 记录自增ID
 * @param bot 机器人ID
 * @param fromId 起始用户ID
 * @param targetId 目标用户ID
 * @param ids 消息ID
 * @param internalIds 消息SEQ
 * @param time Unix时间戳，秒单位
 * @param kind 消息类型
 * @param code 消息内容，JSON序列化
 * @param recalled 撤销者
 * @property recall 已撤销
 */
@Entity
@Table(name = "message_record", indexes = [Index(columnList = "from_id"), Index(columnList = "target_id")])
@Serializable
public data class MessageRecord(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "bot", nullable = false, updatable = false)
    val bot: Long,
    @Column(name = "from_id", nullable = false, updatable = false)
    val fromId: Long,
    @Column(name = "target_id", nullable = false, updatable = false)
    val targetId: Long,
    @Column(name = "ids", nullable = true, updatable = false)
    val ids: String?,
    @Column(name = "internal_ids", nullable = true, updatable = false)
    val internalIds: String?,
    @Column(name = "time", nullable = false, updatable = false)
    val time: Int,
    @Column(name = "kind", nullable = false, updatable = false)
    @Enumerated(value = EnumType.ORDINAL)
    val kind: MessageSourceKind,
    @Column(name = "code", nullable = false, updatable = false, columnDefinition = "text")
    val code: String,
    @Column(name = "recall", nullable = false, updatable = true)
    @Enumerated(value = EnumType.ORDINAL)
    @Serializable(RecalledKind.Serializer::class)
    val recalled: RecalledKind = RecalledKind.NONE
) : java.io.Serializable {
    /**
     * [MessageSource.originalMessage] 来自 [MessageRecord.code] 的解码
     */
    public fun toMessageSource(): MessageSource {
        return Mirai.buildMessageSource(bot, kind) {
            fromId = this@MessageRecord.fromId
            targetId = this@MessageRecord.targetId
            ids = this@MessageRecord.ids.toIntArray()
            internalIds = this@MessageRecord.internalIds.toIntArray()
            time = this@MessageRecord.time
            messages(messages = toMessageChain())
        }
    }

    /**
     * 从 [MessageRecord.code] 解码
     */
    public fun toMessageChain(): MessageChain {
        return try {
            MessageChain.deserializeFromJsonString(code)
        } catch (cause: SerializationException) {
            try {
                MiraiCode.deserializeMiraiCode(code)
            } catch (_: Throwable) {
                throw cause
            }
        }
    }

    /**
     * 获取消息记录对应发送人名称
     * @param subject 上下文
     * @since 2.7
     */
    @JvmOverloads
    public fun name(subject: Contact? = null): String {
        when (recalled) {
            RecalledKind.NONE -> Unit
            RecalledKind.SEND_FAIL -> return "send-fail"
            RecalledKind.SELF -> return "self-recalled"
            RecalledKind.ADMIN -> return "admin-recalled"
        }
        val context = subject ?: when (kind) {
            MessageSourceKind.GROUP -> Bot.getInstanceOrNull(qq = bot)?.getGroup(id = targetId)
            MessageSourceKind.FRIEND -> Bot.getInstanceOrNull(qq = bot)?.getFriend(id = targetId)
            MessageSourceKind.TEMP -> Bot.getInstanceOrNull(qq = bot)?.getFriend(id = targetId)
            MessageSourceKind.STRANGER -> Bot.getInstanceOrNull(qq = bot)?.getStranger(id = targetId)
        }
        val sender = when (context) {
            is Group -> if (fromId == bot) context.botAsMember else context[fromId]
            is Friend -> if (fromId == bot) context.bot else context
            is Stranger -> if (fromId == bot) context.bot else context
            null -> if (fromId == bot) Bot.getInstanceOrNull(qq = bot) else null
            else -> null
        }
        return sender?.nameCardOrNick ?: runCatching(::remark).getOrNull() ?: "$fromId"
    }

    private fun remark(): String? {
        return when (kind) {
            MessageSourceKind.GROUP -> factory.fromSession { session ->
                val record = session.withCriteria<GroupMemberRecord> { query ->
                    val root = query.from<GroupMemberRecord>()
                    val index = root.get<GroupMemberIndex>("uuid")
                    query.select(root)
                        .where(
                            equal(index, GroupMemberIndex(targetId, fromId))
                        )
                }.singleResultOrNull
                record?.name
            }
            MessageSourceKind.FRIEND -> factory.fromSession { session ->
                val record = session.withCriteria<FriendRecord> { query ->
                    val root = query.from<FriendRecord>()
                    val index = root.get<FriendIndex>("uuid")
                    query.select(root)
                        .where(
                            equal(index, FriendIndex(bot, fromId))
                        )
                }.singleResultOrNull
                record?.remark
            }
            MessageSourceKind.TEMP -> factory.fromSession { session ->
                val record = session.withCriteria<GroupMemberRecord> { query ->
                    val root = query.from<GroupMemberRecord>()
                    val index = root.get<GroupMemberIndex>("uuid")
                    val uid = index.get<Long>("uid")
                    query.select(root)
                        .where(
                            equal(uid, targetId)
                        )
                }.singleResultOrNull
                record?.name
            }
            MessageSourceKind.STRANGER -> null
        }
    }

    @get:jakarta.persistence.Transient
    public val recall: Boolean get() = recalled != RecalledKind.NONE

    public companion object {
        /**
         * From Success Send Message
         */
        public fun fromSuccess(source: MessageSource, message: MessageChain): MessageRecord = MessageRecord(
            bot = source.botId,
            fromId = source.fromId,
            targetId = source.targetId,
            time = source.time,
            ids = source.ids.joinToString(","),
            internalIds = source.internalIds.joinToString(","),
            kind = source.kind,
            code = with(MessageChain) {
                message.serializeToJsonString()
            }
        )

        /**
         * From Failure Send Message
         */
        public fun fromFailure(target: Contact, message: MessageChain): MessageRecord = MessageRecord(
            bot = target.bot.id,
            fromId = target.bot.id,
            targetId = target.id,
            time = 0,
            ids = null,
            internalIds = null,
            kind = when (target) {
                is Group -> MessageSourceKind.GROUP
                is Friend -> MessageSourceKind.FRIEND
                is Member -> MessageSourceKind.TEMP
                is Stranger -> MessageSourceKind.STRANGER
                else -> throw NoSuchElementException("Unknown message kind with $target")
            },
            code = with(MessageChain) {
                message.serializeToJsonString()
            },
            recalled = RecalledKind.SEND_FAIL
        )

        private fun String?.toIntArray(): IntArray {
            return if (isNullOrEmpty()) {
                IntArray(0)
            } else {
                split(',').mapToIntArray { it.toInt() }
            }
        }
    }
}