package xyz.cssxsh.hibernate

import org.hibernate.dialect.function.*
import org.hibernate.sql.ast.SqlAstTranslator
import org.hibernate.sql.ast.spi.SqlAppender
import org.hibernate.sql.ast.tree.SqlAstNode
import org.hibernate.sql.ast.tree.predicate.Predicate
import org.hibernate.sql.ast.tree.select.SortSpecification
import org.hibernate.type.*

/**
 * 宏函数，通过构造宏来实现自定义的函数方法.
 * @param type 返回值类型，无返回值可以填 null,
 * @param macro 表达式构造 lambda
 * @since 2.3.3
 * @see addRandFunction
 * @see addDiceFunction
 */
public class MacroSQLFunction(
    type: BasicTypeReference<*>? = null,
    private val macro: SqlAppender.(List<SqlAstNode>, SqlAstTranslator<*>) -> Unit
) : StandardSQLFunction("macro", false, type) {

    override fun render(
        sqlAppender: SqlAppender,
        sqlAstArguments: List<SqlAstNode>,
        translator: SqlAstTranslator<*>
    ) {
        macro.invoke(sqlAppender, sqlAstArguments, translator)
    }

    override fun render(
        sqlAppender: SqlAppender,
        sqlAstArguments: List<SqlAstNode>,
        filter: Predicate,
        translator: SqlAstTranslator<*>
    ) {
        render(sqlAppender, sqlAstArguments, translator)
    }

    override fun render(
        sqlAppender: SqlAppender,
        sqlAstArguments: List<SqlAstNode>,
        filter: Predicate?,
        withinGroup: MutableList<SortSpecification>,
        translator: SqlAstTranslator<*>
    ) {
        render(sqlAppender, sqlAstArguments, translator)
    }

    override fun render(
        sqlAppender: SqlAppender,
        sqlAstArguments: List<SqlAstNode>,
        filter: Predicate,
        respectNulls: Boolean,
        fromFirst: Boolean,
        translator: SqlAstTranslator<*>
    ) {
        render(sqlAppender, sqlAstArguments, translator)
    }
}