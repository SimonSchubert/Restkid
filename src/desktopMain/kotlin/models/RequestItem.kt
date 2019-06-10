package models

import kotlinx.serialization.Serializable

@Serializable
data class RequestItem(
        var name: String = "",
        var request: Request = Request()
)