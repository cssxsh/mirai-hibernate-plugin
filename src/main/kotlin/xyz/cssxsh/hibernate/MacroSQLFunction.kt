package xyz.cssxsh.hibernate

import org.hibernate.dialect.function.*
import org.hibernate.query.ReturnableType
import org.hibernate.sql.ast.SqlAstTranslator
import org.hibernate.sql.ast.spi.SqlAppender
import org.hibernate.sql.ast.tree.SqlAstNode
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
        returnType: ReturnableType<*>,
        translator: SqlAstTranslator<*>
    ) {
        macro.invoke(sqlAppender, sqlAstArguments, translator)
    }
}