package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.internal.message.data.*
import net.mamoe.mirai.internal.message.image.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.*

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
    public val width: Int,
    @Column(name = "disable", nullable = false, columnDefinition = "default false")
    public val disable: Boolean = false
) : java.io.Serializable {

    @OneToMany
    @JoinColumn(name = "md5")
    @kotlinx.serialization.Transient
    public val tags: List<FaceTagRecord> = emptyList()

    public fun toMessageContent(): MessageContent {
        val face = json.decodeFromString(serializer, code)
        // XXX: 序列化信息有限
        if (face is Image && !face.isEmoji) {
            return Image(imageId = face.imageId) {
                height = this@FaceRecord.height
                width = this@FaceRecord.height
                type = ImageType.match(face.imageId.substringAfter('.'))
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
        public fun fromImage(image: Image): FaceRecord {
            return FaceRecord(
                md5 = image.md5.toUHexString("").lowercase(),
                code = json.encodeToString(serializer, image),
                content = image.contentToString(),
                height = image.height,
                width = image.width,
                url = runBlocking { image.queryUrl() }
            )
        }

        /**
         * from [MarketFaceImpl]
         */
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        public fun fromMarketFace(face: MarketFace): FaceRecord {
            val delegate = try {
                (face as MarketFaceImpl).delegate
            } catch (_: Throwable) {
                face::class.java.getDeclaredField("delegate")
                    .get(face).cast()
            }
            val md5 = delegate.faceId.toUHexString("").lowercase()
            return FaceRecord(
                md5 = md5,
                code = json.encodeToString(serializer, face),
                height = delegate.imageHeight,
                width = delegate.imageWidth,
                content = face.name,
                url = "https://gxh.vip.qq.com/club/item/parcel/item/${md5.substring(0..1)}/$md5/300x300.png"
            )
        }
    }
}