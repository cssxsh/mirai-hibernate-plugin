package xyz.cssxsh.hibernate

import org.hibernate.*
import javax.persistence.*
import java.io.*

/**
 * @see show
 */
@Entity
public data class MySqlVariable(
    @Id
    @Column(name = "Variable_name", nullable = false)
    val name: String,
    @Column(name = "Value", nullable = false)
    val value: String
): Serializable {
    public companion object SQL {
        @JvmStatic
        public fun Session.show(): List<MySqlVariable> {
            return createNativeQuery<MySqlVariable>("""SHOW VARIABLES""", MySqlVariable::class.java).list()
        }
    }
}