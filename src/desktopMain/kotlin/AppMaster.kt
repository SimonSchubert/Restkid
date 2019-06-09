import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.call.receive
import io.ktor.client.engine.curl.Curl
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.util.toMap
import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.stringify
import platform.posix.*
import kotlin.system.getTimeMillis

@ImplicitReflectionSerializer
class AppMaster(private val callback: Callback) {

    lateinit var collection: Api

    fun start() {
        val collections = getSavedCollections()
        callback.showMainApp(collections, { u, m, b, h ->
            makeRequest(u, m, b, h)
        }, { index ->
            val path = getCollectionsPath()
            loadCollection("$path/${collections[index]}.json")
        }, { path ->
            loadCollection(path)
        }, {
            showVariablesWindow()
        })
        if (collections.size > 0) {
            collectionComboBox.value = 0
            val path = getCollectionsPath()
            loadCollection("$path/${collections[0]}.json")
        }
    }

    interface Callback {
        fun showResponse(body: String, headers: String, stats: String)
        fun showCollection(collection: Api, click: (RequestItem) -> Unit)
        fun showRequest(url: String, method: String, headers: List<RequestItemHeader>, body: String)
        fun showVariablesWindow(collection: Api, save: (Api) -> Unit, remove: (Int) -> Unit, new: () -> Unit)
        fun showNewVariableSetWindow(collection: Api, save: (String) -> Unit)
        fun showSaveDialog(save: () -> Unit)
        fun showMainApp(collections: List<String>, request: (u: String, m: HttpMethod, b: String, h: Map<String, String>) -> Unit, loadByIndex: (Int) -> Unit, loadByPath: (String) -> Unit, showVariables: () -> Unit)
    }

    private fun makeRequest(u: String, m: HttpMethod, b: String, h: Map<String, String>) {
        var url = u
        memScoped {
            runBlocking {
                try {
                    collection.variables.forEach {
                        it.variables.forEach {
                            url = url.replace("{{${it.key}}}", it.value)
                        }
                    }

                    val startTime = getTimeMillis()

                    val client = HttpClient(Curl)
                    val call = client.call(url) {
                        method = m

                        h.forEach {
                            if (it.key == "Content-Type") {
                                body = TextContent(b, contentType = ContentType.Application.Json)
                            } else {
                                headers.append(it.key, it.value)
                            }
                        }
                    }

                    val response = call.response.receive<HttpResponse>()

                    if (response.status == HttpStatusCode.OK) {
                        val text = response.readText()
                        val body = try {
                            val json = Json.nonstrict.parseJson(text)
                            Json.indented.stringify(json)
                        } catch (e: Exception) {
                            text
                        }

                        val headerList = response.headers.toMap().toList()
                        val headers = headerList.joinToString(separator = "\n") {
                            it.first + " = " + it.second
                        }
                        val contentSize = (headerList.firstOrNull { it.first == "Content-Length" }?.second?.get(0)?.toLong()
                                ?: 0) / 1024f
                        val stats = "Status: ${response.status.value} ${response.status.description} / Time: ${getTimeMillis() - startTime} ms / Size: $contentSize KB"

                        callback.showResponse(body, headers, stats)
                    } else {
                        val stats = "Status: ${response.status.value} ${response.status.description} / Time: ${getTimeMillis() - startTime} ms"

                        callback.showResponse("", "", stats)
                    }

                    response.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadCollection(path: String) {
        val file = fopen(path, "r")
        try {
            var content = ""
            memScoped {
                val bufferLength = 64 * 1024
                val buffer = allocArray<ByteVar>(bufferLength)
                while (true) {
                    val nextLine = fgets(buffer, bufferLength, file)?.toKString()
                    if (nextLine == null || nextLine.isEmpty()) break
                    content += nextLine
                }

                collection = Json.nonstrict.parse(Api.serializer(), content)
                callback.showCollection(collection) {
                    callback.showRequest(it.request.url, it.request.method, it.request.headers, it.request.body)
                }
            }
        } finally {
            fclose(file)
        }
    }

    private fun saveCollection() {
        val path = getCollectionsPath()
        mkdir(path, S_IRWXU)

        val name = collection.info.name
        val fileName = "$path/$name.json"
        val file = fopen(fileName, "wt")
        if (file == null) throw Error("Cannot write file '$fileName'")
        try {
            val json = Json.stringify(Api.serializer(), collection)
            fputs(json, file)
        } finally {
            fclose(file)
        }
    }

    private fun showVariablesWindow() {
        if (collection.variables.isEmpty()) {
            showNewVariableSetWindow()
            return
        }
        callback.showVariablesWindow(collection, {
            saveCollection()
        }, {
            collection.variables.removeAt(it)
            showVariablesWindow()
        }, {
            showNewVariableSetWindow()
        })
    }

    private fun showNewVariableSetWindow() {
        callback.showNewVariableSetWindow(collection) { name ->
            if (collection.variables.any { it.name == name }) {

            } else {
                val variablesSet = VariableSet(name, listOf())
                collection.variables.add(variablesSet)
            }
        }
    }

    private fun getStorageDir(): String {
        val home = getenv("HOME")?.toKString() ?: ""
        return "$home/Restkid/"
    }

    private fun getCollectionsPath(): String {
        return "${getStorageDir()}collections/"
    }

    private fun getSavedCollections(): MutableList<String> {
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
}