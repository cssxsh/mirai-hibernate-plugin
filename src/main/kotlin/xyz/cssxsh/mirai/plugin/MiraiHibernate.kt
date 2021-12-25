package xyz.cssxsh.mirai.plugin

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class MiraiHibernate(
    val loader: KClass<out MiraiHibernateLoader>
)