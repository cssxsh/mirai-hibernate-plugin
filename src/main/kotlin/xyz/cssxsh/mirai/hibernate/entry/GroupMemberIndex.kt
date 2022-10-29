package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*

/**
 * @since 2.5.0
 */
@Embeddable
public data class GroupMemberIndex(
    @Column(name = "group_id", nullable = false, updatable = false)
    val group: Long,
    @Column(name = "uid", nullable = false, updatable = false)
    val uid: Long
) : java.io.Serializable
