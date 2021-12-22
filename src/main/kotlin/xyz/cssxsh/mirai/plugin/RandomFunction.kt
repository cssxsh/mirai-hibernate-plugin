package xyz.cssxsh.mirai.plugin

import org.hibernate.query.criteria.internal.*
import org.hibernate.query.criteria.internal.expression.function.*
import java.io.*

class RandomFunction(criteriaBuilder: CriteriaBuilderImpl) :
    BasicFunctionExpression<Double>(criteriaBuilder, Double::class.java, "rand"), Serializable