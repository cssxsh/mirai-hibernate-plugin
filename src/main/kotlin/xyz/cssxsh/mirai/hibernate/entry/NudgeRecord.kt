package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*

/**
 * 戳一戳记录
 * @param id 记录自增ID
 * @param bot 机器人ID
 * @param time Unix时间戳，秒单位
 * @param fromId 起始用户ID
 * @param targetId 目标用户ID
 * @param kind 消息类型
 * @param subject 会话所在ID
 * @param action 戳一戳 `行为`
 * @param suffix 戳一戳 `后缀`
 * @param recalled 撤销者
 * @property recall 已撤销
 */
@Entity
@Table(name = "nudge_record")
@Serializable
public data class NudgeRecord(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "bot", nullable = false, updatable = false)
    val bot: Long,
    @Column(name = "time", nullable = false, updatable = false)
    val time: Int,
    @Column(name = "from_id", nullable = false, updatable = false)
    val fromId: Long,
    @Column(name = "target_id", nullable = false, updatable = false)
    val targetId: Long,
    @Column(name = "kind", nullable = false, updatable = false)
    @Enumerated(value = EnumType.ORDINAL)
    val kind: MessageSourceKind,
    @Column(name = "subject", nullable = false, updatable = false)
    val subject: Long,
    @Column(name = "action", nullable = false, updatable = false)
    val action: String,
    @Column(name = "suffix", nullable = false, updatable = false)
    val suffix: String,
    @Serializable(RecalledKind.Serializer::class)
    @Enumerated(value = EnumType.ORDINAL)
    val recalled: RecalledKind = RecalledKind.NONE
) : java.io.Serializable {
    public constructor(event: NudgeEvent, time: Int = (System.currentTimeMillis() / 1000).toInt()) : this(
        bot = event.bot.id,
        time = time,
        fromId = event.from.id,
        targetId = event.target.id,
        kind = when (event.subject) {
            is Group -> MessageSourceKind.GROUP
            is Friend -> MessageSourceKind.FRIEND
            is Member -> MessageSourceKind.TEMP
            is Stranger -> MessageSourceKind.STRANGER
            else -> throw NoSuchElementException("Nudge kind with ${event.subject}")
        },
        subject = event.subject.id,
        action = event.action,
        suffix = event.suffix
    )

    public val recall: Boolean get() = recalled != RecalledKind.NONE
}