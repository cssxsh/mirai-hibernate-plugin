package xyz.cssxsh.hibernate

import jakarta.persistence.criteria.*
import org.hibernate.*
import org.hibernate.cfg.*
import org.hibernate.dialect.function.*
import org.hibernate.query.*
import org.hibernate.sql.ast.SqlAstTranslator
import org.hibernate.sql.ast.spi.SqlAppender
import org.hibernate.sql.ast.tree.SqlAstNode
import org.hibernate.sql.ast.tree.predicate.Predicate
import org.hibernate.sql.ast.tree.select.SortSpecification
import org.hibernate.type.*
import java.sql.*

/**
 * rand 函数
 * @see Configuration.addRandFunction
 */
public fun CriteriaBuilder.rand(): Expression<Double> = function("rand", Double::class.java)

/**
 * Sqlite / PostgreSQL 添加 rand 函数别名
 *
 * 通过 `hibernate.connection.url` 判断数据库类型，然后添加对应的函数别名
 *
 * @see CriteriaBuilder.rand
 */
public fun Configuration.addRandFunction() {
    // MySql rand 0 ~ 1
    // Sqlite random -9223372036854775808 ~ +9223372036854775807
    // PostgreSQL random 0 ~ 1
    // H2 rand 0 ~ 1
    // SqlServer rand 0 ~ 1
    val url = getProperty("hibernate.connection.url") ?: return
    when {
        url.startsWith("jdbc:sqlite") -> {
            addSqlFunction("rand", object : StandardSQLFunction("rand", StandardBasicTypes.DOUBLE) {
                override fun render(
                    sqlAppender: SqlAppender,
                    sqlAstArguments: MutableList<out SqlAstNode>?,
                    translator: SqlAstTranslator<*>?
                ) {
                    sqlAppender.appendSql("((RANDOM() + 9223372036854775808) / 2.0 / 9223372036854775808)")
                }

                override fun render(
                    sqlAppender: SqlAppender,
                    sqlAstArguments: MutableList<out SqlAstNode>?,
                    filter: Predicate?,
                    translator: SqlAstTranslator<*>?
                ) {
                    render(sqlAppender, sqlAstArguments, translator)
                }

                override fun render(
                    sqlAppender: SqlAppender,
                    sqlAstArguments: MutableList<out SqlAstNode>?,
                    filter: Predicate?,
                    withinGroup: MutableList<SortSpecification>?,
                    translator: SqlAstTranslator<*>?
                ) {
                    render(sqlAppender, sqlAstArguments, translator)
                }

                override fun render(
                    sqlAppender: SqlAppender,
                    sqlAstArguments: MutableList<out SqlAstNode>?,
                    filter: Predicate?,
                    respectNulls: Boolean?,
                    fromFirst: Boolean?,
                    translator: SqlAstTranslator<*>?
                ) {
                    render(sqlAppender, sqlAstArguments, translator)
                }
            })
        }
        url.startsWith("jdbc:postgresql") -> {
            addSqlFunction("rand", StandardSQLFunction("random", StandardBasicTypes.DOUBLE))
        }
    }
}

/**
 * 构造一个 Criteria 查询
 */
public inline fun <reified T> Session.withCriteria(block: CriteriaBuilder.(criteria: CriteriaQuery<T>) -> Unit): Query<T> =
    createQuery(with(criteriaBuilder) { createQuery(T::class.java).also { block(it) } })

/**
 * 构造一个 Criteria 查询
 */
public inline fun <reified T> Session.withCriteriaUpdate(block: CriteriaBuilder.(criteria: CriteriaUpdate<T>) -> Unit): MutationQuery =
    createMutationQuery(with(criteriaBuilder) { createCriteriaUpdate(T::class.java).also { block(it) } })

public inline fun <reified X> CriteriaQuery<*>.from(): Root<X> = from(X::class.java)

public inline fun <reified T> CriteriaUpdate<T>.from(): Root<T> = from(T::class.java)

/**
 * 获取会话的 [DatabaseMetaData]
 */
public fun Session.getDatabaseMetaData(): DatabaseMetaData = doReturningWork { connection -> connection.metaData }