package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * @since 2.5.0
 */
@Embeddable
@Serializable(FriendIndex.Companion::class)
public data class FriendIndex(
    @Column(name = "bot", nullable = false, updatable = false)
    val bot: Long,
    @Column(name = "uid", nullable = false, updatable = false)
    val uid: Long
) : java.io.Serializable {
    internal companion object : KSerializer<FriendIndex> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor(FriendIndex::class.qualifiedName!!, PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): FriendIndex {
            val uuid = decoder.decodeString()
            val (bot, uid) = uuid.split(".")
            return FriendIndex(
                bot = bot.toLong(),
                uid = uid.toLong()
            )
        }

        override fun serialize(encoder: Encoder, value: FriendIndex) {
            encoder.encodeString("${value.bot}.${value.uid}")
        }
    }
}
