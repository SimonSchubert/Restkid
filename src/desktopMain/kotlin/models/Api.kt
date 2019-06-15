package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Api(
        var info: ApiInfo = ApiInfo(),
        @SerialName("item")
        var groups: MutableList<RequestGroup> = mutableListOf(),
        var variables: MutableList<VariableSet> = mutableListOf())