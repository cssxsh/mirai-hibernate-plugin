package xyz.cssxsh.mirai.hibernate.entry

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * 撤回类型
 * @since 2.7
 */
public enum class RecalledKind {
    NONE, SEND_FAIL, SELF, ADMIN;

    public companion object Serializer : KSerializer<RecalledKind> {

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor(this::class.qualifiedName!!, PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): RecalledKind {
            val index = decoder.decodeInt()
            return values().getOrNull(index) ?: SEND_FAIL
        }

        override fun serialize(encoder: Encoder, value: RecalledKind) {
            encoder.encodeInt(value.ordinal)
        }
    }
}