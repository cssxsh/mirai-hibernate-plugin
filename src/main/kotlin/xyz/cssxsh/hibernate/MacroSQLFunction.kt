package xyz.cssxsh.hibernate

import org.hibernate.dialect.function.*
import org.hibernate.sql.ast.SqlAstTranslator
import org.hibernate.sql.ast.spi.SqlAppender
import org.hibernate.sql.ast.tree.SqlAstNode
import org.hibernate.sql.ast.tree.predicate.Predicate
import org.hibernate.sql.ast.tree.select.SortSpecification
import org.hibernate.type.*

public class MacroSQLFunction(
    name: String,
    type: BasicTypeReference<*>? = null,
    private val expression: SqlAppender.(List<SqlAstNode>, SqlAstTranslator<*>) -> Unit
) : StandardSQLFunction(name, false, type) {

    override fun render(
        sqlAppender: SqlAppender,
        sqlAstArguments: List<SqlAstNode>,
        translator: SqlAstTranslator<*>
    ) {
        expression.invoke(sqlAppender, sqlAstArguments, translator)
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