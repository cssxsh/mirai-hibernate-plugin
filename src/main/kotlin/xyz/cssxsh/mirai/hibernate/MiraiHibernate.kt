package xyz.cssxsh.mirai.hibernate

import kotlin.reflect.*

@Deprecated("mirai 2.11.0 之后的版本更新了类加载机制，此注解无法正确加载")
@Target(AnnotationTarget.CLASS)
public annotation class MiraiHibernate(
    val loader: KClass<out MiraiHibernateLoader>
)