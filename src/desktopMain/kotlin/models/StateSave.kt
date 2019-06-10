package models

import kotlinx.serialization.Serializable

@Serializable
data class StateSave(
        var selection: Int = 0
)