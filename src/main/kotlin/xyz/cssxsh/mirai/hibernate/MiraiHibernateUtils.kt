package xyz.cssxsh.mirai.hibernate

import net.mamoe.mirai.contact.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.*
import org.hibernate.*
import org.hibernate.dialect.Dialect
import org.sqlite.SQLiteJDBCLoader
import xyz.cssxsh.hibernate.*
import xyz.cssxsh.mirai.hibernate.entry.*
import java.io.File
import java.net.URL

internal val logger by lazy {
    try {
        MiraiHibernatePlugin.logger
    } catch (_: ExceptionInInitializerError) {
        MiraiLogger.Factory.create(MiraiHibernateRecorder::class)
    }
}

private const val SQLITE_JNI =
    "https://raw.githubusercontent.com/xerial/sqlite-jdbc/master/src/main/resources/org/sqlite/native/Linux-Android/aarch64/libsqlitejdbc.so"

/**
 * 检查当前平台，并修正问题
 * @see SQLiteJDBCLoader
 */
public fun checkPlatform(folder: File) {
    // Termux
    if ("termux" in System.getProperty("user.dir")) {
        logger.info { "change platform to Linux-Android" }
        System.setProperty("org.sqlite.lib.path", folder.path)
        val lib = folder.resolve("libsqlitejdbc.so")
        if (lib.exists().not()) {
            val url = SQLiteJDBCLoader::class.java.classLoader
                .getResource("org/sqlite/native/Linux-Android/aarch64/libsqlitejdbc.so")
                ?: URL(SQLITE_JNI)

            lib.writeBytes(url.readBytes())
        }
        SQLiteJDBCLoader.initialize()
    }
}

/**
 * 获取所有 [Dialect]
 * @see Dialect
 */
public fun dialects(): Set<Class<out Dialect>> {
    return org.reflections.Reflections("org.hibernate.dialect", "org.hibernate.community.dialect")
        .getSubTypesOf(Dialect::class.java)
}

/**
 * 获取 MSSQL 驱动 artifact id
 *
 * version by Property xyz.cssxsh.mirai.hibernate.mssql.version
 */
@PublishedApi
internal fun mssql(): String {
    val java = System.getProperty("java.version")
    val version = System.getProperty("xyz.cssxsh.mirai.hibernate.mssql.version", "11.2.3")
    return when {
        java.startsWith("17") -> "com.microsoft.sqlserver:mssql-jdbc:${version}.jre17"
        java.startsWith("11") -> "com.microsoft.sqlserver:mssql-jdbc:${version}.jre11"
        else -> "com.microsoft.sqlserver:mssql-jdbc:${version}.jre11"
    }
}

/**
 * 插件的 SessionFactory
 */
public lateinit var factory: SessionFactory
    internal set

/**
 * 将消息记录打包为转发消息
 * @param subject 上下文
 * @see ForwardMessage
 */
public fun List<MessageRecord>.toForwardMessage(subject: Contact): ForwardMessage {
    return buildForwardMessage(subject) {
        for (record in this@toForwardMessage) {
            record.fromId at record.time says record.toMessageChain()
        }
    }
}

/**
 * 将消息记录打包为转发消息
 * @param subject 上下文
 * @see ForwardMessage
 */
public fun Sequence<MessageRecord>.toForwardMessage(subject: Contact): ForwardMessage {
    return buildForwardMessage(subject) {
        for (record in this@toForwardMessage) {
            record.fromId at record.time says record.toMessageChain()
        }
    }
}

/**
 * 随机得到一个表情包记录
 * @see factory
 */
public fun FaceRecord.Companion.random(): FaceRecord {
    return factory.fromSession { session ->
        val count = session.withCriteria<Long> { query ->
            val record = query.from<FaceRecord>()
            query.select(count(record))
        }.uniqueResult().toInt()
        logger.debug { "face record count $count" }
        session.withCriteria<FaceRecord> { query ->
            val record = query.from<FaceRecord>()
            query.select(record)
                .where(not(record.get("disable")))
                .orderBy(desc(record.get<String>("md5")))
        }.setFirstResult((0 until count).random()).setMaxResults(1).uniqueResult()
    }
}

/**
 * 禁用指定 [md5] 的 表情包记录
 * @see FaceRecord.md5
 */
public fun FaceRecord.Companion.disable(md5: String): FaceRecord {
    return factory.fromTransaction { session ->
        val result = session.withCriteriaUpdate<FaceRecord> { query ->
            val root = query.from()
            query.where(equal(root.get<String>("md5"), md5))
                .set(root.get("disable"), true)
        }.executeUpdate()

        check(result > 0) { "FaceRecord(${md5}).disable 修改失败" }

        session.get(FaceRecord::class.java, md5)
    }
}

/**
 * 通过 [tag] 获取表情包记录
 * @see FaceTagRecord.tag
 */
public fun FaceRecord.Companion.match(tag: String): List<FaceRecord> {
    return factory.fromSession { session ->
        session.withCriteria<FaceRecord> { query ->
            val root = query.from<FaceRecord>()
            val join = root.joinList<FaceRecord, FaceTagRecord>("tags")
            query.select(root)
                .where(equal(join.get<String>("tag"), tag))
        }.list()
    }
}

/**
 * 通过 [md5] 获取表情包标签记录
 * @see FaceTagRecord.md5
 */
public operator fun FaceTagRecord.Companion.get(md5: String): List<FaceTagRecord> {
    return factory.fromSession { session ->
        session.withCriteria<FaceTagRecord> { query ->
            val root = query.from<FaceTagRecord>()
            query.select(root)
                .where(equal(root.get<String>("md5"), md5))
        }.list()
    }
}

/**
 * 通过 [md5] 设置表情包标签记录
 * @see FaceTagRecord.md5
 */
public operator fun FaceTagRecord.Companion.set(md5: String, tag: String): List<FaceTagRecord> {
    return factory.fromTransaction { session ->
        session.persist(FaceTagRecord(md5 = md5, tag = tag))

        session.withCriteria<FaceTagRecord> { query ->
            val root = query.from<FaceTagRecord>()
            query.select(root)
                .where(equal(root.get<String>("md5"), md5))
        }.list()
    }
}

/**
 * 通过 [md5] 移除表情包标签记录
 * @see FaceTagRecord.md5
 */
public fun FaceTagRecord.Companion.remove(md5: String, tag: String): List<FaceTagRecord> {
    return factory.fromTransaction { session ->
        session.withCriteriaDelete<FaceTagRecord> { query ->
            val root = query.from()
            query.where(
                equal(root.get<String>("md5"), md5),
                equal(root.get<String>("tag"), tag)
            )
        }.executeUpdate()

        session.withCriteria<FaceTagRecord> { query ->
            val root = query.from<FaceTagRecord>()
            query.select(root)
                .where(equal(root.get<String>("md5"), md5))
        }.list()
    }
}