package models

import kotlinx.serialization.Serializable

/**
 * Collection info class
 */
@Serializable
data class ApiInfo(
        var name: String = "",
        var description: String = "")