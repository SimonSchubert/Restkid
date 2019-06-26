import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonParsingException
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.stringify

/**
 * Format bytes to human readable format (e.g. 54.12 KB)
 */
internal fun Long.formatToHumanReadableSize(): String {
    var bytes = this
    val dictionary = arrayOf("bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")
    var index = 0
    while (index < dictionary.size) {
        if (bytes < 1024) {
            break
        }
        bytes /= 1024
        index++
    }
    var string = bytes.toString()
    val pointIndex = string.indexOf(".")
    if (string.length > pointIndex + 3) {
        string = string.substring(0, pointIndex + 3)
    }
    return string + " " + dictionary[index]
}

/**
 * Indent json to make it better readable
 */
@ImplicitReflectionSerializer
internal fun String.prettyJson(): String {
    return try {
        val json = Json.nonstrict.parseJson(this)
        Json.indented.stringify(json)
    } catch (ignore: JsonParsingException) {
        this
    }
}

/**
 * Get string by key or empty string
 */
internal fun JsonObject.optString(key: String): String {
    return this[key]?.contentOrNull ?: ""
}