package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestGroup(
        var name: String = "",
        var description: String = "",
        @SerialName("item")
        var items: List<RequestItem>
)