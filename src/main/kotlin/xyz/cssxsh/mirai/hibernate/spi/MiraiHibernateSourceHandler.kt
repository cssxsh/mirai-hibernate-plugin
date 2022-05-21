package xyz.cssxsh.mirai.hibernate.spi

import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import xyz.cssxsh.mirai.hibernate.*
import xyz.cssxsh.mirai.spi.*

public class MiraiHibernateSourceHandler : MessageSourceHandler {
    override val id: String = "hibernate-recorder"
    override val level: Int by lazy { System.getProperty("xyz.cssxsh.mirai.hibernate.recorder", "10").toInt() }

    private fun records(contact: Contact) = MiraiHibernateRecorder[contact].filterNot { it.recall }

    override fun find(contact: Contact?, event: MessageEvent?): MessageSource? {
        return when {
            contact is Member -> {
                records(contact).firstOrNull()?.toMessageSource()
            }
            contact != null -> {
                records(contact).find { it.bot == it.fromId }?.toMessageSource()
            }
            event != null -> {
                event.message.findIsInstance<QuoteReply>()?.source
                    ?: records(event.subject).find { it.fromId != event.sender.id }?.toMessageSource()
            }
            else -> null
        }
    }
}