package xyz.cssxsh.hibernate

import jakarta.persistence.criteria.*
import org.hibernate.*
import org.hibernate.cfg.*
import org.hibernate.dialect.function.*
import org.hibernate.query.*
import org.hibernate.sql.ast.*
import org.hibernate.type.*
import java.sql.*

/**
 * rand 函数
 * @see Configuration.addRandFunction
 */
public fun CriteriaBuilder.rand(): Expression<Double> = function("rand", Double::class.java)

/**
 * dice 函数
 * @param model 模，应为正整数
 * @return 随机生成 [0, model) 范围的数
 * @since 2.3.3
 * @see Configuration.addRandFunction
 */
public fun CriteriaBuilder.dice(model: Expression<Long>): Expression<Long> = function("dice", Long::class.java, model)

/**
 * Sqlite / PostgreSQL 添加 rand 函数别名
 *
 * 通过 `hibernate.connection.url` 判断数据库类型，然后添加对应的函数别名
 * @since 2.3.3
 * @see CriteriaBuilder.rand
 */
public fun Configuration.addRandFunction() {
    // MySql rand 0 ~ 1
    // Sqlite random -9223372036854775808 ~ +9223372036854775807
    // PostgreSQL random 0 ~ 1
    // H2 rand 0 ~ 1
    // SqlServer rand 0 ~ 1
    val url = getProperty("hibernate.connection.url") ?: throw IllegalStateException("url is empty")
    when {
        url.startsWith("jdbc:sqlite") -> {
            addSqlFunction("rand", MacroSQLFunction(StandardBasicTypes.DOUBLE) { _, _ ->
                appendSql("((RANDOM() + 9223372036854775808) / 2.0 / 9223372036854775808)")
            })
        }
        url.startsWith("jdbc:postgresql") -> {
            addSqlFunction("rand", StandardSQLFunction("random", StandardBasicTypes.DOUBLE))
        }
    }
}

/**
 * 添加 dice 函数
 *
 * 通过 `hibernate.connection.url` 判断数据库类型，然后添加对应的函数别名
 *
 * @see CriteriaBuilder.dice
 */
public fun Configuration.addDiceFunction() {
    // MySql rand 0 ~ 1
    // Sqlite random -9223372036854775808 ~ +9223372036854775807
    // PostgreSQL random 0 ~ 1
    // H2 rand 0 ~ 1
    // SqlServer rand 0 ~ 1
    val url = getProperty("hibernate.connection.url") ?: throw IllegalStateException("url is empty")
    when {
        url.startsWith("jdbc:sqlite") -> {
            addSqlFunction("dice", MacroSQLFunction(StandardBasicTypes.LONG) { args, translator ->
                val (model) = args
                appendSql("ABS(RANDOM() % ")
                translator.render(model, SqlAstNodeRenderingMode.DEFAULT)
                appendSql(")")
            })
        }
        url.startsWith("jdbc:postgresql") -> {
            addSqlFunction("dice", MacroSQLFunction(StandardBasicTypes.LONG) { args, translator ->
                val (model) = args
                appendSql("FLOOR(")
                translator.render(model, SqlAstNodeRenderingMode.DEFAULT)
                appendSql(" * RANDOM())")
            })
        }
        else -> {
            addSqlFunction("dice", MacroSQLFunction(StandardBasicTypes.LONG) { args, translator ->
                val (model) = args
                appendSql("FLOOR(")
                translator.render(model, SqlAstNodeRenderingMode.DEFAULT)
                appendSql(" * RAND())")
            })
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

/**
 * 构造一个 Criteria 查询
 */
public inline fun <reified T> Session.withCriteriaDelete(block: CriteriaBuilder.(criteria: CriteriaDelete<T>) -> Unit): MutationQuery =
    createMutationQuery(with(criteriaBuilder) { createCriteriaDelete(T::class.java).also { block(it) } })

public inline fun <reified X> AbstractQuery<*>.from(): Root<X> = from(X::class.java)

public inline fun <reified T> CriteriaUpdate<T>.from(): Root<T> = from(T::class.java)

public inline fun <reified T> CriteriaDelete<T>.from(): Root<T> = from(T::class.java)

public inline fun <reified T> CommonAbstractCriteria.subquery(): Subquery<T> = subquery(T::class.java)

/**
 * 获取会话的 [DatabaseMetaData]
 */
public fun Session.getDatabaseMetaData(): DatabaseMetaData = doReturningWork { connection -> connection.metaData }