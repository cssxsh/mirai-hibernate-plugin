package xyz.cssxsh.mirai.hibernate.entry

import java.io.File

class MySqlTest : DatabaseTest() {
    init {
        configuration.apply {
            File("./example/mysql.hibernate.properties").inputStream().use(properties::load)
        }
    }
}