package xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.console.plugin.jvm.*
import org.hibernate.*
import org.hibernate.query.Query
import org.hibernate.query.criteria.internal.*
import javax.persistence.criteria.*

internal val logger get() = MiraiHibernatePlugin.logger

internal val currentSession get() = MiraiHibernatePlugin.factory.currentSession

val JvmPlugin.factory: SessionFactory get() = MiraiSessionCache[this]

internal fun <R> useSession(lock: Any? = null, block: (session: Session) -> R): R {
    return if (lock == null) {
        MiraiHibernatePlugin.factory.openSession().use(block)
    } else {
        synchronized(lock) {
            MiraiHibernatePlugin.factory.openSession().use(block)
        }
    }
}

fun CriteriaBuilder.rand() = RandomFunction(this as CriteriaBuilderImpl)

inline fun <reified T> Session.withCriteria(block: CriteriaBuilder.(criteria: CriteriaQuery<T>) -> Unit): Query<T> =
    createQuery(with(criteriaBuilder) { createQuery(T::class.java).also { block(it) } })

inline fun <reified T> Session.withCriteriaUpdate(block: CriteriaBuilder.(criteria: CriteriaUpdate<T>) -> Unit): Query<*> =
    createQuery(with(criteriaBuilder) { createCriteriaUpdate(T::class.java).also { block(it) } })