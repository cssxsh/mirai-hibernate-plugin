package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*

/**
 * 好友记录
 * @param uuid 好友索引
 * @param remark 备注
 * @param category 好友分组
 * @param added 添加时间戳
 * @param deleted 删除时间戳
 * @since 2.5.0
 */
@Entity
@Table(name = "friend_record")
@Serializable
public data class FriendRecord(
    @EmbeddedId
    val uuid: FriendIndex,
    @Column(name = "remark", nullable = false)
    val remark: String,
    @Column(name = "category", nullable = false)
    val category: String,
    @Column(name = "added", nullable = false, updatable = false)
    val added: Long,
    @Column(name = "deleted", nullable = false)
    val deleted: Long
) : java.io.Serializable {

    public companion object {
        /**
         * From Friend Event
         */
        public fun fromEvent(event: FriendEvent): FriendRecord = FriendRecord(
            uuid = FriendIndex(bot = event.bot.id, uid = event.friend.id),
            remark = event.friend.remarkOrNick,
            category = try {
                event.friend.friendGroup.name
            } catch (_: Throwable) {
                "我的好友"
            },
            added = if (event is FriendAddEvent) System.currentTimeMillis() / 1_000 else 0,
            deleted = if (event is FriendDeleteEvent) System.currentTimeMillis() / 1_000 else Long.MAX_VALUE,
        )

        /**
         * From Friend Implement
         */
        public fun fromImpl(friend: Friend): FriendRecord = FriendRecord(
            uuid = FriendIndex(bot = friend.bot.id, uid = friend.id),
            remark = friend.remarkOrNick,
            category = try {
                friend.friendGroup.name
            } catch (_: Throwable) {
                "我的好友"
            },
            added = 0,
            deleted = Long.MAX_VALUE,
        )
    }
}