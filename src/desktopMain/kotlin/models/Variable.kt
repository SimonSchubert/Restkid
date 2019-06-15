package models

import kotlinx.serialization.Serializable

/**
 * Variable key value class
 */
@Serializable
data class Variable(
        var key: String = "",
        var value: String = ""
)