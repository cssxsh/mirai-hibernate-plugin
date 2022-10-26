package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*

@Entity
@Table(name = "friend_record")
@Serializable
public data class FriendRecord(
    @jakarta.persistence.Transient
    val bot: Long,
    @jakarta.persistence.Transient
    val uid: Long,
    @Column(name = "remark", nullable = false)
    val remark: String,
    @Column(name = "group_name", nullable = false)
    val group: String,
    @Column(name = "added", nullable = false, updatable = false)
    val added: Long,
    @Column(name = "deleted", nullable = false)
    val deleted: Long
) : java.io.Serializable {
    @EmbeddedId
    @kotlinx.serialization.Transient
    val index: FriendIndex = FriendIndex(bot = bot, uid = uid)

    public companion object {
        public fun fromEvent(event: FriendEvent): FriendRecord = FriendRecord(
            bot = event.bot.id,
            uid = event.friend.id,
            remark = event.friend.remarkOrNick,
            group = try {
                event.friend.friendGroup.name
            } catch (_: Throwable) {
                "我的好友"
            },
            added = if (event is FriendAddEvent) System.currentTimeMillis() / 1_000 else 0,
            deleted = if (event is FriendDeleteEvent) System.currentTimeMillis() / 1_000 else Long.MAX_VALUE,
        )

        public fun fromImpl(friend: Friend): FriendRecord = FriendRecord(
            bot = friend.bot.id,
            uid = friend.id,
            remark = friend.remarkOrNick,
            group = try {
                friend.friendGroup.name
            } catch (_: Throwable) {
                "我的好友"
            },
            added = 0,
            deleted = Long.MAX_VALUE,
        )
    }
}