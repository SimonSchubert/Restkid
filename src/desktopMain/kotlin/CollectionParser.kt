import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElementTypeMismatchException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.list
import kotlinx.serialization.stringify
import models.*

/**
 * Postman collections can be structured in such different ways that makes it impossible to use the standard ktlinx.serialization parser.
 *
 * Request items may be structured like this:
 * {"url": "https://example.com", "body": "example"}
 * OR
 * {"url": {"raw": "https://example.com", "schema": "https"}, "body": {"raw": "https://example.com"}
 *
 * Also items may be wrapped inside a group or not.
 */
class CollectionParser {

    /**
     * Manually parse raw postman json string to Api object.
     */
    @ImplicitReflectionSerializer
    fun parseCollection(content: String): Api {
        val json = Json.nonstrict.parseJson(content).jsonObject
        val api = Api()
        json[KEY_INFO]?.let {
            api.info = Json.nonstrict.parse(ApiInfo.serializer(), Json.indented.stringify(it))
        }
        json[KEY_VARIABLES]?.let {
            api.variables = Json.nonstrict.parse(VariableSet.serializer().list, Json.indented.stringify(it)).toMutableList()
        }

        json.getArrayOrNull(KEY_ITEM)?.map { it.jsonObject }?.forEach { groupJson ->
            if (groupJson.containsKey(KEY_REQUEST)) {
                val group = api.groups.firstOrNull() ?: RequestGroup()
                val item = parseRequestItem(groupJson)
                group.items.add(item)
                if (api.groups.isEmpty()) {
                    api.groups.add(group)
                }
            } else {
                val group = RequestGroup()

                group.name = groupJson.optString(KEY_NAME)
                group.description = groupJson.optString(KEY_DESCRIPTION)

                groupJson.getArrayOrNull(KEY_ITEM)?.forEach { itemJson ->
                    val item = parseRequestItem(itemJson.jsonObject)
                    group.items.add(item)
                }

                api.groups.add(group)
            }
        }
        return api
    }

    private fun parseRequestItem(jsonObject: JsonObject): RequestItem {
        val item = RequestItem()
        item.name = jsonObject.optString(KEY_NAME)

        jsonObject.getObjectOrNull(KEY_REQUEST)?.let { requestJson ->
            val request = Request()
            try {
                request.url = requestJson[KEY_URL]?.jsonObject?.optString(KEY_RAW) ?: ""
            } catch (e: JsonElementTypeMismatchException) {
                request.url = requestJson.optString(KEY_URL)
            }
            try {
                request.body = requestJson[KEY_BODY]?.jsonObject?.optString(KEY_RAW) ?: ""
            } catch (e: JsonElementTypeMismatchException) {
                request.body = requestJson.optString(KEY_BODY)
            }
            request.method = requestJson.optString(KEY_METHOD)
            request.description = requestJson.optString(KEY_DESCRIPTION)
            requestJson.getArrayOrNull(KEY_HEADER)?.forEach {
                val header = RequestItemHeader()
                header.key = it.jsonObject.optString(KEY_KEY)
                header.value = it.jsonObject.optString(KEY_VALUE)
                header.description = it.jsonObject.optString(KEY_DESCRIPTION)
                request.headers.add(header)
            }
            item.request = request
        }
        return item
    }

    companion object {
        const val KEY_BODY = "body"
        const val KEY_RAW = "raw"
        const val KEY_URL = "url"
        const val KEY_ITEM = "item"
        const val KEY_NAME = "name"
        const val KEY_REQUEST = "request"
        const val KEY_DESCRIPTION = "description"
        const val KEY_METHOD = "method"
        const val KEY_HEADER = "header"
        const val KEY_KEY = "key"
        const val KEY_VALUE = "value"
        const val KEY_VARIABLES = "variables"
        const val KEY_INFO = "info"
    }
}