package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Api(
        var info: ApiInfo,
        @SerialName("item")
        var groups: List<RequestGroup>,
        var variables: MutableList<VariableSet> = mutableListOf())