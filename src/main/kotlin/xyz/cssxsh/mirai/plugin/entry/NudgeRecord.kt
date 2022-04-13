package xyz.cssxsh.mirai.plugin.entry

import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import java.io.*
import javax.persistence.*

@Entity
@Table(name = "nudge_record")
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
    val kind: Int,
    @Column(name = "subject", nullable = false, updatable = false)
    val subject: Long,
    @Column(name = "action", nullable = false, updatable = false)
    val action: String,
    @Column(name = "suffix", nullable = false, updatable = false)
    val suffix: String,
    @Column(name = "recall", nullable = false)
    val recall: Boolean = false
) : Serializable {
    public constructor(event: NudgeEvent, time: Int = (System.currentTimeMillis() / 1000).toInt()) : this(
        bot = event.bot.id,
        time = time,
        fromId = event.from.id,
        targetId = event.target.id,
        kind = when (event.subject) {
            is Group -> MessageSourceKind.GROUP.ordinal
            is Friend -> MessageSourceKind.FRIEND.ordinal
            is Member -> MessageSourceKind.TEMP.ordinal
            is Stranger -> MessageSourceKind.STRANGER.ordinal
            else -> throw NoSuchElementException("Nudge kind with ${event.subject}")
        },
        subject = event.subject.id,
        action = event.action,
        suffix = event.suffix
    )
}