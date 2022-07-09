package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*

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
    @Column(name = "recall", nullable = false)
    val recall: Boolean = false
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
}