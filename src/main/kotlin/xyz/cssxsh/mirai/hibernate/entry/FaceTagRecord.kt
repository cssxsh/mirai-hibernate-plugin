package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*

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
    public val tag: String,
) : java.io.Serializable