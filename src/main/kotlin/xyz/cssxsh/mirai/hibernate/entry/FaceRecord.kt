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
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.*

/**
 * 表情包记录
 * @param md5 图片MD5, 同时用作ID
 * @param code 消息内容，JSON序列
 * @param content 图片代替文本
 * @param url 图片URL
 * @param height 图片高
 * @param width 图片宽
 * @param disable 禁用
 */
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
    @org.hibernate.annotations.ColumnDefault("'FALSE'")
    public val disable: Boolean = false
) : java.io.Serializable {

    /**
     * 表情包标签记录集
     * @see md5
     */
    @OneToMany(mappedBy = "md5")
    @kotlinx.serialization.Transient
    public val tags: List<FaceTagRecord> = emptyList()

    /**
     * [FaceRecord.code] 解码
     * @see code
     */
    public fun toMessageContent(): MessageContent = json.decodeFromString(serializer, code)

    public companion object {
        @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
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
            val size = if (delegate.tabId < 10_0000) 200 else 300
            val url = when (delegate.subType) {
                1 -> "https://gxh.vip.qq.com/club/item/parcel/item/${md5.substring(0, 2)}/$md5/raw${size}.gif"
                2 -> "https://gxh.vip.qq.com/club/item/parcel/item/${md5.substring(0, 2)}/$md5/raw${size}.png"
                3 -> "https://gxh.vip.qq.com/club/item/parcel/item/${md5.substring(0, 2)}/$md5/raw${size}.gif"
                else -> "https://gxh.vip.qq.com/club/item/parcel/item/${md5.substring(0, 2)}/$md5/${size}x${size}.png"
            }
            return FaceRecord(
                md5 = md5,
                code = json.encodeToString(serializer, face),
                height = delegate.imageHeight,
                width = delegate.imageWidth,
                content = face.name,
                url = url
            )
        }
    }
}