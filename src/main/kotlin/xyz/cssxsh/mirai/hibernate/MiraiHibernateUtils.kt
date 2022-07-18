package xyz.cssxsh.mirai.hibernate

import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import org.hibernate.*
import org.sqlite.SQLiteJDBCLoader
import xyz.cssxsh.hibernate.*
import xyz.cssxsh.mirai.hibernate.entry.*
import java.io.File
import java.net.URL

internal val logger by lazy {
    try {
        MiraiHibernatePlugin.logger
    } catch (_: Throwable) {
        MiraiLogger.Factory.create(MiraiHibernateRecorder::class)
    }
}

private const val SQLITE_JNI =
    "https://raw.fastgit.org/xerial/sqlite-jdbc/master/src/main/resources/org/sqlite/native/Linux-Android/aarch64/libsqlitejdbc.so"

public fun checkPlatform(folder: File) {
    // Termux
    if ("termux" in System.getProperty("user.dir")) {
        logger.info { "change platform to Linux-Android" }
        System.setProperty("org.sqlite.lib.path", folder.path)
        folder.resolve("libsqlitejdbc.so").apply {
            if (exists().not()) {
                val url = SQLiteJDBCLoader::class.java.classLoader
                    .getResource("org/sqlite/native/Linux-Android/aarch64/libsqlitejdbc.so")
                    ?: URL(SQLITE_JNI)

                url.openStream().use { writeBytes(it.readAllBytes()) }
            }
        }
        SQLiteJDBCLoader.initialize()
    }
}

internal lateinit var factory: SessionFactory

internal fun <R> useSession(block: (session: Session) -> R): R {
    return factory.openSession().use { session ->
        val transaction = session.beginTransaction()
        try {
            val result = block.invoke(session)
            transaction.commit()
            result
        } catch (cause: Throwable) {
            transaction.rollback()
            throw cause
        }
    }
}

public fun List<MessageRecord>.toForwardMessage(context: Contact) {
    buildForwardMessage(context) {
        for (record in this@toForwardMessage) {
            record.fromId at record.time says record.toMessageChain()
        }
    }
}

public fun Sequence<MessageRecord>.toForwardMessage(context: Contact) {
    buildForwardMessage(context) {
        for (record in this@toForwardMessage) {
            record.fromId at record.time says record.toMessageChain()
        }
    }
}

public fun FaceRecord.Companion.random(): FaceRecord {
    return useSession { session ->
        val count = session.withCriteria<Long> { criteria ->
            val record = criteria.from<FaceRecord>()
            criteria.select(count(record))
        }.uniqueResult().toInt()
        logger.debug { "face record count $count" }
        session.withCriteria<FaceRecord> { criteria ->
            val record = criteria.from<FaceRecord>()
            criteria.select(record)
                .where(not(record.get("disable")))
                .orderBy(desc(record.get<String>("md5")))
        }.setFirstResult((0 until count).random()).setMaxResults(1).uniqueResult()
    }
}

public fun FaceRecord.Companion.disable(md5: String): FaceRecord {
    return useSession { session ->
        val result = session.withCriteriaUpdate<FaceRecord> { criteria ->
            val root = criteria.from()
            criteria
                .set(root.get("disable"), true)
                .where(equal(root.get<String>("md5"), md5))
        }.executeUpdate()

        check(result > 0) { "FaceRecord(${md5}).disable 修改失败" }

        session.get(FaceRecord::class.java, md5)
    }
}

public fun FaceRecord.Companion.match(tag: String): List<FaceRecord> {
    return useSession { session ->
        session.withCriteria<FaceRecord> { criteria ->
            val root = criteria.from<FaceRecord>()
            val join = root.joinList<FaceRecord, FaceTagRecord>("tags")
            criteria.select(root)
                .where(equal(join.get<String>("tag"), tag))
        }.list()
    }
}

public operator fun FaceTagRecord.Companion.get(md5: String): List<FaceTagRecord> {
    return useSession { session ->
        session.withCriteria<FaceTagRecord> { criteria ->
            val root = criteria.from<FaceTagRecord>()
            criteria.select(root)
                .where(equal(root.get<String>("md5"), md5))
        }.list()
    }
}

public operator fun FaceTagRecord.Companion.set(md5: String, tag: String): List<FaceTagRecord> {
    return useSession { session ->
        session.persist(FaceTagRecord(md5 = md5, tag = tag))

        session.withCriteria<FaceTagRecord> { criteria ->
            val root = criteria.from<FaceTagRecord>()
            criteria.select(root)
                .where(equal(root.get<String>("md5"), md5))
        }.list()
    }
}

public fun FaceTagRecord.Companion.remove(md5: String, tag: String): List<FaceTagRecord> {
    return useSession { session ->
        session.withCriteriaDelete<FaceTagRecord> { criteria ->
            val root = criteria.from()
            criteria
                .where(equal(root.get<String>("md5"), md5))
        }.executeUpdate()

        session.withCriteria<FaceTagRecord> { criteria ->
            val root = criteria.from<FaceTagRecord>()
            criteria.select(root)
                .where(
                    equal(root.get<String>("md5"), md5),
                    equal(root.get<String>("tag"), tag)
                )
        }.list()
    }
}