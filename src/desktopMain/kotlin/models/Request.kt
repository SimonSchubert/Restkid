package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Request(
        // val url: URLUnion? = null,
        var url: String = "",
        var method: String = "",
        var description: String = "",
        var body: String = "",
        @SerialName("header")
        var headers: MutableList<RequestItemHeader> = mutableListOf()
)