package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request data class
 */
@Serializable
data class Request(
        var url: String = "",
        var method: String = "",
        var description: String = "",
        var body: String = "",
        @SerialName("header")
        var headers: MutableList<RequestItemHeader> = mutableListOf()
)