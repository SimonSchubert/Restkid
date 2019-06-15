package models

import kotlinx.serialization.Serializable

/**
 * Variable set class
 */
@Serializable
data class VariableSet(
        var name: String = "",
        var variables: List<Variable> = listOf()
)