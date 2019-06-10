package models

import kotlinx.serialization.Serializable

@Serializable
data class ApiInfo(
        var name: String = "",
        var description: String = "")