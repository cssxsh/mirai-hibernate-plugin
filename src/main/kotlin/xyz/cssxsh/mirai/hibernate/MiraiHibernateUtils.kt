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
                .orderBy(desc(record.get<String>("md5")))
        }.setFirstResult((0 until count).random()).setMaxResults(1).uniqueResult()
    }
}