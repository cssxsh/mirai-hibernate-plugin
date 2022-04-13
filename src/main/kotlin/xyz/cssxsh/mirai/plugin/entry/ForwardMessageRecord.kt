package xyz.cssxsh.mirai.plugin.entry

import net.mamoe.mirai.message.data.ForwardMessage
import java.io.*
import javax.persistence.*

@Entity
@Table(name = "forward_message_record")
public data class ForwardMessageRecord(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @JoinColumn(name = "record")
    @OneToOne(optional = false, fetch = FetchType.EAGER)
    val record: MessageRecord,
    @Column(name = "preview", nullable = true, updatable = false)
    val preview: String = "",
    @Column(name = "title", nullable = true, updatable = false)
    val title: String = "",
    @Column(name = "brief", nullable = true, updatable = false)
    val brief: String = "",
    @Column(name = "source", nullable = true, updatable = false)
    val source: String = "",
    @Column(name = "summary", nullable = true, updatable = false)
    val summary: String = "",
) : Serializable {
    public constructor(record: MessageRecord, forward: ForwardMessage) : this(
        record = record,
        preview = forward.preview.joinToString("\n"),
        title = forward.title,
        brief = forward.brief,
        source = forward.source,
        summary = forward.summary
    )

    @OneToMany(mappedBy = "forward", fetch = FetchType.LAZY)
    public val nodes: List<ForwardNodeRecord> = emptyList()

    public fun toForwardMessage(): ForwardMessage {
        return ForwardMessage(
            preview = preview.split('\n'),
            title = title,
            brief = brief,
            source = source,
            summary = summary,
            nodeList = nodes.map { record -> record.toNode() }
        )
    }
}