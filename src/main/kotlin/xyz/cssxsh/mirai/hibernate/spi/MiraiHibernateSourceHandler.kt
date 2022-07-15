package xyz.cssxsh.mirai.hibernate.spi

import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import xyz.cssxsh.mirai.hibernate.*
import xyz.cssxsh.mirai.spi.*
import kotlin.streams.asSequence

public class MiraiHibernateSourceHandler : MessageSourceHandler {
    override val id: String = "hibernate-recorder"
    override val level: Int by lazy { System.getProperty("xyz.cssxsh.mirai.hibernate.recorder", "10").toInt() }

    @Deprecated(message = "兼容性实现", replaceWith = ReplaceWith("null"))
    override fun find(contact: Contact?, event: MessageEvent?): MessageSource? {
        return when {
            contact is Member -> from(member = contact)
            contact != null -> target(contact = contact)
            event != null -> quote(event = event)
            else -> null
        }
    }

    override fun from(member: Member): MessageSource? {
        return MiraiHibernateRecorder[member].use { stream ->
            stream.asSequence().find { !it.recall }?.toMessageSource()
        }
    }

    override fun target(contact: Contact): MessageSource? {
        return MiraiHibernateRecorder[contact].use { stream ->
            stream.asSequence().find { !it.recall && it.bot == it.fromId }?.toMessageSource()
        }
    }

    override fun quote(event: MessageEvent): MessageSource? {
        val quote = event.message.findIsInstance<QuoteReply>()
        return if (quote != null) {
            MiraiHibernateRecorder[quote.source].find { !it.recall }?.toMessageSource()
        } else {
            MiraiHibernateRecorder[event.subject].use { stream ->
                stream.asSequence().find { !it.recall && it.fromId != event.sender.id }?.toMessageSource()
            }
        }
    }
}