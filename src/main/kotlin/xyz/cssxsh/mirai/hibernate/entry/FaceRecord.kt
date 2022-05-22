package xyz.cssxsh.mirai.hibernate.entry

import kotlinx.serialization.json.*
import kotlinx.serialization.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.internal.message.*
import net.mamoe.mirai.message.*
import okio.ByteString.Companion.toByteString
import javax.persistence.*

@Entity
@Table(name = "face_record")
@Serializable
public data class FaceRecord(
    @Id
    @Column(name = "md5", nullable = false, updatable = false, length = 32)
    public val md5: String,
    @Column(name = "code", nullable = false, updatable = false, columnDefinition = "text")
    public val code: String,
    @Column(name = "content", nullable = false)
    public val content: String,
    @Column(name = "url", nullable = false)
    public val url: String,
    @Column(name = "height", nullable = false)
    public val height: Int,
    @Column(name = "width", nullable = false)
    public val width: Int
) : java.io.Serializable {

    public fun toMessageContent(): MessageContent {
        val face = json.decodeFromString(serializer, code)
        // XXX: 序列化信息有限
        if (face is Image && !face.isEmoji) {
            return Image(imageId = face.imageId) {
                height = this@FaceRecord.height
                width = this@FaceRecord.height
                isEmoji = true
            }
        }
        return face
    }

    public companion object {
        private val json = Json {
            serializersModule = MessageSerializers.serializersModule
            ignoreUnknownKeys = true
        }
        private val serializer = PolymorphicSerializer(MessageContent::class)

        /**
         * from [OnlineImage.isEmoji]
         */
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        public fun fromImage(image: Image): FaceRecord {
            return FaceRecord(
                md5 = image.md5.toByteString().hex(),
                code = json.encodeToString(serializer, image),
                content = image.contentToString(),
                height = image.height,
                width = image.width,
                url = (image as OnlineImage).originUrl
            )
        }

        /**
         * from [MarketFaceImpl]
         */
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        public fun fromMarketFace(face: MarketFace): FaceRecord {
            val md5 = (face as MarketFaceImpl).delegate.faceId.toByteString().hex()
            return FaceRecord(
                md5 = md5,
                code = json.encodeToString(serializer, face),
                height = face.delegate.imageHeight,
                width = face.delegate.imageWidth,
                content = face.name,
                url = "https://gxh.vip.qq.com/club/item/parcel/item/${md5.substring(0..1)}/$md5/300x300.png"
            )
        }
    }
}