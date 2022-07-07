package xyz.cssxsh.mirai.hibernate.entry

import xyz.cssxsh.hibernate.*
import java.io.File

class SqliteTest : DatabaseTest() {
    init {
        configuration.apply {
            File("./example/sqlite.hibernate.properties").inputStream().use(properties::load)
            addRandFunction()
        }
    }
}