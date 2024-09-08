package xyz.cssxsh.mirai.hibernate

import kotlinx.coroutines.*
import net.mamoe.mirai.console.events.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.utils.*
import org.h2.jdbc.*
import org.h2.server.web.*
import org.h2.tools.*
import org.hibernate.*
import java.sql.*
import kotlin.coroutines.*

public object MiraiH2 : SimpleListenerHost() {

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (exception) {
            is ExceptionInEventHandlerException -> {
                logger.warning({ "Exception in H2" }, exception.cause)
            }
            else -> {
                logger.warning({ "Exception in H2" }, exception)
            }
        }
    }

    private val web = WebServer()

    /**
     * 创建一个 H2 网络会话
     * @return URL
     * @exception SQLException
     */
    public fun url(session: Session): String {
        return session.doReturningWork { wrapper ->
            val connection = wrapper.unwrap(JdbcConnection::class.java)
            web.addSession(connection)
        }
    }

    @EventHandler
    internal fun ConsoleEvent.handle() {
        if (this !is StartupEvent) return
        web.init("-webPort", System.getProperty("h2.web.port", "0"))
        web.start()
        launch {
            web.listen()
        }

        launch {
            while (web.isRunning(false).not()) {
                delay(timeMillis = 10_000)
            }
            val url = try {
                factory.fromSession { url(session = it) }
            } catch (_: SQLException) {
                return@launch
            }
            logger.info(message = "h2 web $url")
            Server.openBrowser(url)
        }
    }
}