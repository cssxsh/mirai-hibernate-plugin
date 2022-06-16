package xyz.cssxsh.hibernate

import jakarta.persistence.criteria.*
import org.hibernate.*
import org.hibernate.cfg.*
import org.hibernate.dialect.function.*
import org.hibernate.query.*
import org.hibernate.type.*
import java.sql.*

/**
 * rand 函数
 * @see Configuration.addRandFunction
 */
public fun CriteriaBuilder.rand(): Expression<Double> = function("rand", Double::class.java)

/**
 * Sqlite 添加 rand 函数别名
 * @see CriteriaBuilder.rand
 */
public fun Configuration.addRandFunction() {
    addSqlFunction("rand", StandardSQLFunction("random", StandardBasicTypes.DOUBLE))
}

/**
 * 构造一个 Criteria 查询
 */
public inline fun <reified T> Session.withCriteria(block: CriteriaBuilder.(criteria: CriteriaQuery<T>) -> Unit): SelectionQuery<T> =
    createSelectionQuery(with(criteriaBuilder) { createQuery(T::class.java).also { block(it) } })

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