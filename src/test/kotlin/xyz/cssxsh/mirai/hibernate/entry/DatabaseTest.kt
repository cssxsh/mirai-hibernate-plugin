package xyz.cssxsh.mirai.hibernate.entry

import net.mamoe.mirai.message.data.MessageSourceKind
import net.mamoe.mirai.utils.md5
import net.mamoe.mirai.utils.toByteArray
import net.mamoe.mirai.utils.toUHexString
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import xyz.cssxsh.hibernate.rand
import xyz.cssxsh.hibernate.withCriteria

abstract class DatabaseTest {

    protected val configuration = Configuration().apply {
        addAnnotatedClass(FaceRecord::class.java)
        addAnnotatedClass(MessageRecord::class.java)
        addAnnotatedClass(NudgeRecord::class.java)
    }

    protected val factory: SessionFactory by lazy { configuration.buildSessionFactory() }

    @BeforeAll
    fun insert() {
        factory.openSession().use { session ->
            repeat(10) { index ->
                val md5 = index.toByteArray().md5().toUHexString("")
                val record = FaceRecord(
                    md5 = md5,
                    code = "{}",
                    content = "${index}",
                    url = "https://127.0.0.1/$index",
                    height = index,
                    width = index
                )

                session.merge(record)

                MessageRecord(
                    bot = index * 10L,
                    fromId = index * 100L,
                    targetId = index * 1000L,
                    ids = "$index",
                    internalIds ="$index",
                    time = (System.currentTimeMillis() / 1000).toInt(),
                    kind = MessageSourceKind.values().random().ordinal,
                    code = md5
                )

                session.merge(record)
            }
        }
    }

    @Test
    fun rand() {
        val num = factory.openSession().use { session ->
            session.withCriteria<Double> { criteria ->
                criteria.select(rand())
            }.uniqueResult()
        }
        Assertions.assertTrue(num >= 0.0, "< 0.0")
        Assertions.assertTrue(num <= 1.0, "> 1.0")
    }

    @AfterAll
    fun close() {
        factory.close()
    }
}