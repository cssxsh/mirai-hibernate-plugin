package xyz.cssxsh.mirai.hibernate.entry

import java.io.File

class PostgreSqlTest : DatabaseTest() {
    init {
        configuration.apply {
            File("./example/postgresql.hibernate.properties").inputStream().use(properties::load)
        }
    }
}