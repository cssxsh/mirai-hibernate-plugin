package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*

/**
 * 群成员记录
 * @param uuid 好友索引
 * @param permission 权限
 * @param name 名字
 * @param title 头衔
 * @param joined 加入时间戳
 * @param last 最后发言时间戳
 * @param active 活跃度
 * @param exited 退出时间戳
 * @since 2.5.0
 */
@Entity
@Table(name = "group_member_record")
@Serializable
public data class GroupMemberRecord(
    @EmbeddedId
    val uuid: GroupMemberIndex,
    @Column(name = "permission", nullable = false)
    @Enumerated(value = EnumType.ORDINAL)
    val permission: MemberPermission = MemberPermission.MEMBER,
    @Column(name = "name", nullable = false)
    val name: String,
    @Column(name = "title", nullable = false)
    val title: String,
    @Column(name = "joined", nullable = false, updatable = false)
    val joined: Long,
    @Column(name = "last", nullable = false)
    val last: Long,
    @Column(name = "active", nullable = false)
    val active: Int,
    @Column(name = "exited", nullable = false)
    val exited: Long
) : java.io.Serializable {

    public companion object {
        /**
         * From Group Member Event
         */
        public fun fromEvent(event: GroupMemberEvent): GroupMemberRecord = GroupMemberRecord(
            uuid = GroupMemberIndex(group = event.group.id, uid = event.member.id),
            permission = event.member.permission,
            name = (event.member as NormalMember).nameCardOrNick,
            title = (event.member as NormalMember).specialTitle,
            joined = (event.member as NormalMember).joinTimestamp.toLong(),
            last = (event.member as NormalMember).lastSpeakTimestamp.toLong(),
            active = try {
                event.member.active.temperature
            } catch (_: Throwable) {
                0
            },
            exited = when (event) {
                is MemberLeaveEvent -> System.currentTimeMillis() / 1_000
                else -> Long.MAX_VALUE
            }
        )

        /**
         * From Normal Member Implement
         */
        public fun fromImpl(member: NormalMember): GroupMemberRecord = GroupMemberRecord(
            uuid = GroupMemberIndex(group = member.group.id, uid = member.id),
            permission = member.permission,
            name = member.nameCardOrNick,
            title = member.specialTitle,
            joined = member.joinTimestamp.toLong(),
            last = member.lastSpeakTimestamp.toLong(),
            active = try {
                member.active.temperature
            } catch (_: Throwable) {
                0
            },
            exited = Long.MAX_VALUE
        )
    }
}