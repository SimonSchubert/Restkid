package models

import kotlinx.serialization.Serializable

/**
 * Request item class
 */
@Serializable
data class RequestItem(
        var name: String = "",
        var request: Request = Request()
)