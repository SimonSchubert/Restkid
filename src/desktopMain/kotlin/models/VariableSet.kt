package models

import kotlinx.serialization.Serializable

@Serializable
data class VariableSet(
        var name: String = "",
        var variables: List<Variable> = listOf()
)