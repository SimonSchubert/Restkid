package models

import kotlinx.serialization.Serializable

@Serializable
data class Variable(
        var key: String = "",
        var value: String = ""
)