package models

import kotlinx.serialization.Serializable

/**
 * Variable set class
 */
@Serializable
data class VariableSet(
        var name: String = "",
        var variables: MutableList<Variable> = mutableListOf()
)