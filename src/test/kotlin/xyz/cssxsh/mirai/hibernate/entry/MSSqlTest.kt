package xyz.cssxsh.mirai.hibernate.entry

import java.io.File

class MSSqlTest : DatabaseTest() {
    init {
        configuration.apply {
            File("./example/sqlserver.hibernate.properties").inputStream().use(properties::load)

            if (System.getenv("CI") == "true") {
                setProperty("hibernate.connection.password", System.getenv("COMPUTERNAME"))
            }
        }
    }
}