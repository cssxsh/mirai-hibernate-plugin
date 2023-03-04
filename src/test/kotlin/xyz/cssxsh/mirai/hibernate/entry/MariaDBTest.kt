package xyz.cssxsh.mirai.hibernate.entry

import java.io.File

class MariaDBTest : DatabaseTest() {
    init {
        configuration.apply {
            File("./example/mariadb.hibernate.properties").inputStream().use(properties::load)
        }
    }
}