package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*

/**
 * @since 2.5.0
 */
@Entity
@Table(name = "group_member_record")
@Serializable
public data class GroupMemberRecord(
    @jakarta.persistence.Transient
    val group: Long,
    @jakarta.persistence.Transient
    val uid: Long,
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
    @EmbeddedId
    @kotlinx.serialization.Transient
    val index: GroupMemberIndex = GroupMemberIndex(group = group, uid = uid)

    public companion object {
        /**
         * From Group Member Event
         */
        public fun fromEvent(event: GroupMemberEvent): GroupMemberRecord = GroupMemberRecord(
            group = event.group.id,
            uid = event.member.id,
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
            group = member.group.id,
            uid = member.id,
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