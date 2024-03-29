package xyz.cssxsh.mirai.hibernate.entry

import jakarta.persistence.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.junit.jupiter.api.*
import xyz.cssxsh.hibernate.*
import java.io.File
import java.util.ServiceLoader
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class DatabaseTest {

    protected val logger: MiraiLogger = MiraiLogger.Factory.create(this::class.java)

    init {
        ServiceLoader.load(java.sql.Driver::class.java)
            .forEach { driver ->
                logger.info { "Driver: ${driver::class.java.name} Version ${driver.majorVersion}.${driver.minorVersion}" }
            }
    }

    protected val configuration = Configuration().apply {
        val reflections = org.reflections.Reflections("xyz.cssxsh.mirai.hibernate.entry")
        val query = org.reflections.scanners.Scanners.TypesAnnotated
            .of(Entity::class.java, Embeddable::class.java, MappedSuperclass::class.java)
            .asClass<java.io.Serializable>()
        query.apply(reflections.store).forEach { clazz ->
            addAnnotatedClass(clazz)
        }

        setProperty("hibernate.show_sql", "true")
    }

    protected val factory: SessionFactory by lazy {
        File("./data/xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin").mkdirs()
        configuration.addRandFunction()
        configuration.addDiceFunction()
        configuration.buildSessionFactory()
    }

    @BeforeAll
    fun insert() {
        val random = Random(seed = System.currentTimeMillis())
        factory.fromTransaction { session ->
            repeat(100) { index ->
                val md5 = random.nextBytes(16).toUHexString("")
                val face = FaceRecord(
                    md5 = md5,
                    code = "{}",
                    content = "$index",
                    url = "https://127.0.0.1/$index",
                    height = index,
                    width = index
                )

                session.persist(face)

                val message = MessageRecord(
                    bot = index * 10L,
                    fromId = index * 100L,
                    targetId = index * 1000L,
                    ids = "$index",
                    internalIds = "$index",
                    time = (System.currentTimeMillis() / 1000).toInt(),
                    kind = MessageSourceKind.values().random(),
                    code = md5
                )

                session.persist(message)

                val friend = FriendRecord(
                    uuid = FriendIndex(
                        bot = random.nextLong(0, Long.MAX_VALUE),
                        uid = random.nextLong(0, Long.MAX_VALUE)
                    ),
                    remark = "好友",
                    category = "我的好友",
                    added = random.nextLong(),
                    deleted = Long.MAX_VALUE
                )

                session.persist(friend)

                val member = GroupMemberRecord(
                    uuid = GroupMemberIndex(
                        group = random.nextLong(0, Long.MAX_VALUE),
                        uid = random.nextLong(0, Long.MAX_VALUE)
                    ),
                    permission = MemberPermission.values().random(),
                    name = "...",
                    title = "😁",
                    joined = random.nextLong(),
                    last = System.currentTimeMillis(),
                    active = index,
                    exited = Long.MAX_VALUE
                )

                session.persist(member)
            }
        }
    }

    @Test
    fun backup() {
        factory.fromSession { session ->
            session.withCriteria<FriendRecord> { query ->
                val root = query.from<FriendRecord>()
                query.select(root)
            }.list()
            session.withCriteria<GroupMemberRecord> { query ->
                val root = query.from<GroupMemberRecord>()
                query.select(root)
            }.list()
        }
    }

    @Test
    fun rand() {
        val num = factory.fromSession { session ->
            session.withCriteria<Double> { query ->
                query.select(rand())
            }.uniqueResult()
        }
        logger.info("rand $num")
        Assertions.assertTrue(num >= 0.0, "< 0.0")
        Assertions.assertTrue(num <= 1.0, "> 1.0")

        val list = factory.fromSession { session ->
            session.withCriteria<FaceRecord> { query ->
                val record = query.from<FaceRecord>()
                query.select(record)
                    .orderBy(asc(rand()))
            }.setMaxResults(3).list()
        }
        Assertions.assertEquals(list.size, 3)
    }

    @Test
    fun dice() {
        val num = factory.fromSession { session ->
            session.withCriteria<Long> { query ->
                query.select(dice(literal(1000)))
            }.uniqueResult()
        }
        logger.info("dice $num")
        Assertions.assertTrue(num >= 0, "< 0")
        Assertions.assertTrue(num <= 1000, "> 1000")

        val list = factory.fromSession { session ->
            session.withCriteria<MessageRecord> { query ->
                val record = query.from<MessageRecord>()
                val id = record.get<Long>("id")
                val max = query.subquery<Long>().apply {
                    select(max(from<MessageRecord>().get("id")))
                }

                query.select(record)
                    .where(
                        ge(id, dice(max))
                    )
            }.setMaxResults(3).list()
        }
        Assertions.assertEquals(list.size, 3)
    }

    @Test
    fun join() {
        factory.inSession { session ->
            val face = session.withCriteria<FaceRecord> { query ->
                val root = query.from<FaceRecord>()
                query.select(root)
            }.setMaxResults(1).uniqueResult()

            session.transaction.begin()
            session.merge(FaceTagRecord(md5 = face.md5, tag = "test"))
            session.transaction.commit()

            logger.info(face.tags.toString())

            session.transaction.begin()
            session.merge(face.copy(disable = true))
            session.transaction.commit()
        }
    }

    @AfterAll
    fun close() {
        factory.close()
    }
}