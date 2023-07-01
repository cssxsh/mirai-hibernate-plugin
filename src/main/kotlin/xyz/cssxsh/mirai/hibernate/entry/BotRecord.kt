package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*
import net.mamoe.mirai.*
import java.time.*

/**
 * 机器人记录
 * @param bot Bot ID
 * @param name 名字
 * @param init 启用时间戳
 * @param latest 最晚(登录/下线)时间戳
 * @since 2.6.0
 */
@Entity
@Table(name = "bot_record")
@Serializable
public data class BotRecord(
    @Id
    @Column(name = "bot_id", nullable = false, updatable = false)
    val bot: Long,
    @Column(name = "name", nullable = false)
    val name: String,
    @Column(name = "init_time", nullable = false, updatable = false)
    val init: Long,
    @Column(name = "latest", nullable = false)
    val latest: Long
) : java.io.Serializable {

    public companion object {
        /**
         * From Bot Implement
         */
        public fun fromImpl(bot: Bot): BotRecord = BotRecord(
            bot = bot.id,
            name = bot.nick,
            init = Instant.now().epochSecond,
            latest = Instant.now().epochSecond
        )
    }
}