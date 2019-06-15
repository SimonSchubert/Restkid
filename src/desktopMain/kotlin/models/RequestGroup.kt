package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request group class
 */
@Serializable
data class RequestGroup(
        var name: String = "",
        var description: String = "",
        @SerialName("item")
        var items: MutableList<RequestItem> = mutableListOf()
)