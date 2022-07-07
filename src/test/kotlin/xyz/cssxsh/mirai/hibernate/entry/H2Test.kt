package xyz.cssxsh.mirai.hibernate.entry

import xyz.cssxsh.hibernate.*
import java.io.File

class H2Test : DatabaseTest() {
    init {
        configuration.apply {
            File("./example/h2.hibernate.properties").inputStream().use(properties::load)
            addRandFunction()
        }
    }
}