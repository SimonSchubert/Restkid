package models

import kotlinx.serialization.Serializable

@Serializable
data class RequestItemHeader(
        var key: String = "",
        var value: String = "",
        var description: String = ""
)