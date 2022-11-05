package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*

/**
 * 表情包标签记录
 * @param id 记录自增ID
 * @param md5 图片MD5, 同时用作外键关联
 * @param tag 标签
 */
@Entity
@Table(name = "face_tag_record")
@Serializable
public data class FaceTagRecord(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "md5", nullable = false, updatable = false, length = 32)
    public val md5: String,
    @Column(name = "tag", nullable = false, updatable = false, columnDefinition = "text")
    public val tag: String
) : java.io.Serializable