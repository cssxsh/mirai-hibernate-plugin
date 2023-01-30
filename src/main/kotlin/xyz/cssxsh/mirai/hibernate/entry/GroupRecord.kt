package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*
import net.mamoe.mirai.contact.*

/**
 * 群记录
 * @param group 群ID
 * @param name 名字
 * @param owner 群主
 * @param disable 已封禁
 * @since 2.6.0
 */
@Entity
@Table(name = "group_record")
@Serializable
public data class GroupRecord(
    @Id
    @Column(name = "group_id", nullable = false, updatable = false)
    val group: Long,
    @Column(name = "name", nullable = false)
    val name: String,
    @Column(name = "owner", nullable = false)
    val owner: Long,
    @Column(name = "disable", nullable = false)
    val disable: Boolean = false
) : java.io.Serializable {

    public companion object {
        /**
         * From Group Implement
         */
        public fun fromImpl(group: Group): GroupRecord = GroupRecord(
            group = group.id,
            name = group.name,
            owner = group.owner.id
        )
    }
}