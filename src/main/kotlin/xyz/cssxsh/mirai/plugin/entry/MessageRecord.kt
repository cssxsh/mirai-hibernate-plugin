package xyz.cssxsh.mirai.plugin.entry

import net.mamoe.mirai.*
import net.mamoe.mirai.message.code.*
import net.mamoe.mirai.message.data.*
import javax.persistence.*

@Entity
@Table(name = "message_record")
data class MessageRecord(
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "bot", nullable = false)
    val bot: Long,
    @Column(name = "from_id", nullable = false)
    val fromId: Long,
    @Column(name = "target_id", nullable = true)
    val targetId: Long,
    @Column(name = "ids", nullable = true)
    val ids: String,
    @Column(name = "internal_ids", nullable = true)
    val internalIds: String,
    @Column(name = "time", nullable = false)
    val time: Int,
    @Column(name = "kind", nullable = false)
    val kind: Int,
    @Column(name = "code", nullable = false)
    val code: String
) {
    constructor(source: MessageSource, message: MessageChain) : this(
        bot = source.botId,
        fromId = source.fromId,
        targetId = source.targetId,
        time = source.time,
        ids = source.ids.joinToString(","),
        internalIds = source.internalIds.joinToString(","),
        kind = source.kind.ordinal,
        code = message.serializeToMiraiCode()
    )

    fun toMessageSource(): MessageSource {
        return Bot.getInstance(bot).buildMessageSource(MessageSourceKind.values()[kind]) {
            fromId = this@MessageRecord.fromId
            targetId = this@MessageRecord.targetId
            ids = this@MessageRecord.ids.split(',').map { it.toInt() }.toIntArray()
            internalIds = this@MessageRecord.ids.split(',').map { it.toInt() }.toIntArray()
            time = this@MessageRecord.time
            messages(messages = MiraiCode.deserializeMiraiCode(code))
        }
    }
}