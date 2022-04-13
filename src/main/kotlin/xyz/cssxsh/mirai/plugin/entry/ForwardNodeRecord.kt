package xyz.cssxsh.mirai.plugin.entry

import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.*
import java.io.*
import javax.persistence.*

@Entity
@Table(name = "forward_node_record")
public data class ForwardNodeRecord(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @JoinColumn(name = "forward")
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    val forward: ForwardMessageRecord,
    @Column(name = "sender_id", nullable = false, updatable = false)
    val senderId: Long,
    @Column(name = "time", nullable = false, updatable = false)
    val time: Int,
    @Column(name = "sender_name", nullable = false, updatable = false)
    val senderName: String,
    @Column(name = "code", nullable = false, updatable = false, length = 5120)
    val code: String
) : Serializable {
    public constructor(forward: ForwardMessageRecord, node: ForwardMessage.Node) : this(
        forward = forward,
        senderId = node.senderId,
        time = node.time,
        senderName = node.senderName,
        code = node.messageChain.serializeToMiraiCode()
    )

    public fun toNode(): ForwardMessage.Node {
        return ForwardMessage.Node(
            senderId = senderId,
            time = time,
            senderName = senderName,
            messageChain = code.deserializeMiraiCode()
        )
    }
}