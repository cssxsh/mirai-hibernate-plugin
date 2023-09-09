package xyz.cssxsh.mirai.hibernate.entry

import org.junit.jupiter.api.condition.*
import java.io.File

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class SqliteTest : DatabaseTest() {
    init {
        configuration.apply {
            File("./example/sqlite.hibernate.properties").inputStream().use(properties::load)
        }
    }
}