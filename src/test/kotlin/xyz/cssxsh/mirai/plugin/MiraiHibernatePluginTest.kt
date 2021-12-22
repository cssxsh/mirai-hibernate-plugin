package xyz.cssxsh.mirai.plugin.xyz.cssxsh.mirai.plugin

import net.mamoe.mirai.console.plugin.jvm.*
import xyz.cssxsh.mirai.plugin.*

class MiraiHibernatePluginTest :
    KotlinPlugin(JvmPluginDescription(id = "mirai-hibernate-test", name = "mirai-hibernate-test", version = "0.0.0") {
        dependsOn("xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin", false)
    }) {

    override fun onEnable() {

        /**
         * @see JvmPlugin.factory
         * @see MiraiHibernateConfiguration
         */
        val metadata = factory.openSession().use { session ->
            session.doReturningWork { connection -> connection.metaData }
        }

        println(metadata.driverName)
    }
}