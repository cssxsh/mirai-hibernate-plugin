package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * 群成员索引
 * @param group 群ID
 * @param uid 好友ID
 * @since 2.5.0
 */
@Embeddable
@Serializable(GroupMemberIndex.Companion::class)
public data class GroupMemberIndex(
    @Column(name = "group_id", nullable = false, updatable = false)
    val group: Long,
    @Column(name = "uid", nullable = false, updatable = false)
    val uid: Long
) : java.io.Serializable {
    internal companion object : KSerializer<GroupMemberIndex> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor(FriendIndex::class.qualifiedName!!, PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): GroupMemberIndex {
            val uuid = decoder.decodeString()
            val (bot, uid) = uuid.split(".")
            return GroupMemberIndex(
                group = bot.toLong(),
                uid = uid.toLong()
            )
        }

        override fun serialize(encoder: Encoder, value: GroupMemberIndex) {
            encoder.encodeString("${value.group}.${value.uid}")
        }
    }
}
