import kotlinx.cinterop.*
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonParsingException
import models.Api
import platform.posix.*

/**
 * Handle collection loading and saving
 */
class CollectionManager {

    /**
     * Save collection as json to collection data storage
     */
    internal fun saveCollection(collection: Api) {
        memScoped {
            val path = getCollectionsPath()
            mkdir(path, S_IRWXU)

            val name = collection.info.name
            val fileName = "$path/$name.json"
            val file = fopen(fileName, "wt") ?: return
            try {
                val json = Json.stringify(Api.serializer(), collection)
                fputs(json, file)
            } finally {
                fclose(file)
            }
        }
    }

    /**
     * Load and serialize collection from collection data storage
     */
    @ImplicitReflectionSerializer
    internal fun loadCollection(path: String): Api {
        var collection: Api? = null
        memScoped {
            val file = fopen(path, "r")
            try {
                var content = ""
                val bufferLength = 64 * 1024
                val buffer = allocArray<ByteVar>(bufferLength)
                while (true) {
                    val nextLine = fgets(buffer, bufferLength, file)?.toKString()
                    if (nextLine == null || nextLine.isEmpty()) break
                    content += nextLine
                }

                val parser = CollectionParser()
                collection = parser.parseCollection(content)
            } catch (e: JsonParsingException) {
                e.printStackTrace()
            } finally {
                fclose(file)
            }
        }
        return collection ?: Api()
    }

    /**
     * Get all collection file names without prefix
     */
    internal fun getSavedCollections(): MutableList<String> {
        memScoped {
            val fileNames = mutableListOf<String>()

            val path = getCollectionsPath()
            val dir = opendir(path) ?: return fileNames

            while (true) {
                val result = readdir(dir) ?: break
                val name = result.pointed.d_name.toKString()
                if (!isDir(name) && name.endsWith(".json")) {
                    fileNames.add(name.substring(0, name.length - 5))
                }
            }

            closedir(dir)

            return fileNames
        }
    }

    private fun isDir(path: String): Boolean {
        memScoped {
            val s = alloc<stat>()
            if (stat(path, s.ptr) == 0) {
                return (s.st_mode.toInt() and S_IFDIR) != 0
            } else {
                return false
            }
        }
    }

    private fun getAppStoragePath(): String {
        memScoped {
            val home = getenv("HOME")?.toKString() ?: ""
            return "$home/Restkid/"
        }
    }

    /**
     * Get directory of collection data storage
     */
    internal fun getCollectionsPath(): String {
        return memScoped { "${getAppStoragePath()}collections/" }
    }
}