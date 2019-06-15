package models

import kotlinx.serialization.Serializable

/**
 * Request header key value class
 */
@Serializable
data class RequestItemHeader(
        var key: String = "",
        var value: String = "",
        var description: String = ""
)