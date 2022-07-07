package xyz.cssxsh.mirai.hibernate.entry

import org.junit.jupiter.api.TestInstance
import xyz.cssxsh.hibernate.addRandFunction
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class H2Test : DatabaseTest() {
    init {
        configuration.apply {
            File("./example/h2.hibernate.properties").inputStream().use(properties::load)
            addRandFunction()
        }
    }
}