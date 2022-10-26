package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*

@Embeddable
public data class FriendIndex(
    @Column(name = "bot", nullable = false, updatable = false)
    val bot: Long,
    @Column(name = "uid", nullable = false, updatable = false)
    val uid: Long
) : java.io.Serializable
