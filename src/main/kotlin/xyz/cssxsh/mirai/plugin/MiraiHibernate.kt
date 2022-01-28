package xyz.cssxsh.mirai.plugin

import kotlin.reflect.*

@Target(AnnotationTarget.CLASS)
public annotation class MiraiHibernate(
    val loader: KClass<out MiraiHibernateLoader>
)