package xyz.cssxsh.hibernate

import org.hibernate.*
import javax.persistence.*
import java.io.*

/**
 * @see show
 */
@Entity
public data class SqlitePragma(
    @Id
    @Column(name = "auto_vacuum")
    public val autoVacuum: Int,
    @Id
    @Column(name = "automatic_index")
    public val automaticIndex: Int,
    @Id
    @Column(name = "checkpoint_fullfsync")
    public val checkpointFullfsync: Int,
    @Id
    @Column(name = "foreign_keys")
    public val foreignKeys: Int,
    @Id
    @Column(name = "fullfsync")
    public val fullfsync: Int,
    @Id
    @Column(name = "ignore_check_constraints")
    public val ignoreCheckConstraints: Int,
    @Id
    @Column(name = "journal_mode")
    public val journalMode: Int,
    @Id
    @Column(name = "journal_size_limit")
    public val journalSizeLimit: Int,
    @Id
    @Column(name = "locking_mode")
    public val lockingMode: Int,
    @Id
    @Column(name = "max_page_count")
    public val maxPageCount: Int,
    @Id
    @Column(name = "page_size")
    public val pageSize: Int,
    @Id
    @Column(name = "recursive_triggers")
    public val recursiveTriggers: Int,
    @Id
    @Column(name = "secure_delete")
    public val secureDelete: Int,
    @Id
    @Column(name = "synchronous")
    public val synchronous: Int,
    @Id
    @Column(name = "temp_store")
    public val tempStore: Int,
    @Id
    @Column(name = "user_version")
    public val userVersion: Int
): Serializable {
    public companion object SQL {
        @JvmStatic
        public fun Session.show(): SqlitePragma {
            val pragmas = SqlitePragma::class.java.declaredFields.mapNotNull { it.getAnnotation(Column::class.java)?.name }
            check(pragmas.isNotEmpty()) { "Column 注解有误" }
            return createNativeQuery<SqlitePragma>(
                """SELECT * FROM ${pragmas.joinToString(",") { "pragma_${it}" }}""",
                SqlitePragma::class.java
            ).uniqueResult()
        }
    }
}
