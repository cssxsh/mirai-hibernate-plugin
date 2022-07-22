package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
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
    @Column(name = "code", nullable = false, columnDefinition = "text")
    public val code: String,
    @Column(name = "content", nullable = false)
    public val content: String,
    @Column(name = "url", nullable = false)
    public val url: String,
    @Column(name = "height", nullable = false)
    public val height: Int,
    @Column(name = "width", nullable = false)
    public val width: Int,
    @Column(name = "disable", nullable = false, updatable = false)
    @org.hibernate.annotations.ColumnDefault("False")
    public val disable: Boolean = false
) : java.io.Serializable {

    @OneToMany(mappedBy = "md5")
    @kotlinx.serialization.Transient
    public val tags: List<FaceTagRecord> = emptyList()

    public fun toMessageContent(): MessageContent = json.decodeFromString(serializer, code)

    private object ImageSerializer : KSerializer<Image> {
        private val delegate = ImageDelegate.serializer()

        override val descriptor: SerialDescriptor = delegate.descriptor

        override fun deserialize(decoder: Decoder): Image {
            return delegate.deserialize(decoder).let { data ->
                Image(data.imageId) {
                    width = data.width
                    height = data.height
                    size = data.size
                    type = data.imageType
                    isEmoji = data.isEmoji
                }
            }
        }

        override fun serialize(encoder: Encoder, value: Image) {
            delegate.serialize(
                encoder,
                ImageDelegate(
                    imageId = value.imageId,
                    width = value.width,
                    height = value.height,
                    size = value.size,
                    imageType = value.imageType,
                    isEmoji = value.isEmoji
                )
            )
        }
    }

    public companion object {
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
        private val json = Json {
            serializersModule = SerializersModule {
                polymorphic(MessageContent::class) {
                    subclass(ImageSerializer)
                    subclass(MarketFaceImpl.serializer())
                }
                @OptIn(ExperimentalSerializationApi::class)
                polymorphicDefaultSerializer(MessageContent::class) { content ->
                    when (content) {
                        is MarketFace -> MarketFaceImpl.serializer()
                        else -> ImageSerializer
                    }.cast()
                }
            }
            ignoreUnknownKeys = true
        }
        private val serializer = PolymorphicSerializer(MessageContent::class)

        @Serializable
        @SerialName("Image")
        private data class ImageDelegate(
            val imageId: String,
            val width: Int = 0,
            val height: Int = 0,
            val size: Long = 0,
            val imageType: ImageType = ImageType.UNKNOWN,
            val isEmoji: Boolean = true
        )

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