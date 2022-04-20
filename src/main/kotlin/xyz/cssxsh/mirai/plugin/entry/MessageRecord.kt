package xyz.cssxsh.mirai.plugin.entry

import kotlinx.serialization.*
import net.mamoe.mirai.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.code.*
import net.mamoe.mirai.message.data.*
import javax.persistence.*

@Entity
@Table(name = "message_record")
public data class MessageRecord(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "bot", nullable = false, updatable = false)
    val bot: Long,
    @Column(name = "from_id", nullable = false, updatable = false)
    val fromId: Long,
    @Column(name = "target_id", nullable = true, updatable = false)
    val targetId: Long,
    @Column(name = "ids", nullable = true, updatable = false)
    val ids: String,
    @Column(name = "internal_ids", nullable = true, updatable = false)
    val internalIds: String,
    @Column(name = "time", nullable = false, updatable = false)
    val time: Int,
    @Column(name = "kind", nullable = false, updatable = false)
    val kind: Int,
    @Column(name = "code", nullable = false, updatable = false, length = 65536)
    val code: String,
    @Column(name = "recall", nullable = false, updatable = true)
    val recall: Boolean = false
) : java.io.Serializable {
    /**
     * [MessageSource.originalMessage] 来自 [MessageRecord.code] 的解码
     */
    public fun toMessageSource(): MessageSource {
        return Bot.getInstance(bot).buildMessageSource(MessageSourceKind.values()[kind]) {
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
        } catch (_: SerializationException) {
            MiraiCode.deserializeMiraiCode(code)
        }
    }

    public companion object {
        /**
         * From Success Message
         */
        public fun fromSuccess(source: MessageSource, message: MessageChain): MessageRecord = MessageRecord(
            bot = source.botId,
            fromId = source.fromId,
            targetId = source.targetId,
            time = source.time,
            ids = source.ids.joinToString(","),
            internalIds = source.internalIds.joinToString(","),
            kind = source.kind.ordinal,
            code = with(MessageChain) {
                message.serializeToJsonString()
            }
        )

        /**
         * From Failure Message
         */
        public fun fromFailure(target: Contact, message: MessageChain): MessageRecord = MessageRecord(
            bot = target.bot.id,
            fromId = target.bot.id,
            targetId = target.id,
            time = 0,
            ids = "",
            internalIds = "",
            kind = when (target) {
                is Group -> MessageSourceKind.GROUP.ordinal
                is Friend -> MessageSourceKind.FRIEND.ordinal
                is Member -> MessageSourceKind.TEMP.ordinal
                is Stranger -> MessageSourceKind.STRANGER.ordinal
                else -> throw NoSuchElementException("Fail Message kind with $target")
            },
            code = with(MessageChain) {
                message.serializeToJsonString()
            },
            // XXX: fail send
            recall = true
        )

        private fun String.toIntArray(): IntArray = splitToSequence(',').map { it.toInt() }.toList().toIntArray()
    }
}