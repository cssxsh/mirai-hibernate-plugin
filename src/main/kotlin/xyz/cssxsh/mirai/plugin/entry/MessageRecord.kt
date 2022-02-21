package xyz.cssxsh.mirai.plugin.entry

import net.mamoe.mirai.*
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
    @Column(name = "code", nullable = false, updatable = false, length = 5120)
    val code: String,
    @Column(name = "recall", nullable = false)
    val recall: Boolean = false
) {
    public constructor(source: MessageSource, message: MessageChain) : this(
        bot = source.botId,
        fromId = source.fromId,
        targetId = source.targetId,
        time = source.time,
        ids = source.ids.joinToString(","),
        internalIds = source.internalIds.joinToString(","),
        kind = source.kind.ordinal,
        code = message.serializeToMiraiCode()
    )

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
            messages(messages = MiraiCode.deserializeMiraiCode(code))
        }
    }

    private fun String.toIntArray(): IntArray {
        if (isBlank()) return IntArray(0)
        val list = split(',')

        return IntArray(list.size) { index -> list[index].toInt() }
    }
}