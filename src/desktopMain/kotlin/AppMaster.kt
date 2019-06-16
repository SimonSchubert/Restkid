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
import models.*
import platform.posix.*
import kotlin.system.getTimeMillis

/**
 * This class handles all the logic separated from ui
 */
@ImplicitReflectionSerializer
class AppMaster(private val callback: Callback) {

    lateinit var collection: Api

    fun start() {
        memScoped {
            val collections = getSavedCollections()
            callback.onShowMainApp(collections, { u, m, b, h ->
                makeRequest(u, m, b, h)
            }, { index ->
                val path = getCollectionsPath()
                loadCollection("$path/${collections[index]}.json")
                showCollection()
            }, { path ->
                loadCollection(path)
                showCollection()
            }, {
                showVariablesWindow()
            }, {
                if (collections.size > 0) {
                    uiCollection.value = 0
                    val path = getCollectionsPath()
                    loadCollection("$path/${collections[0]}.json")
                    showCollection()
                }
            }, {
                callback.onShowSaveDialog {
                    saveCollection()
                }
            }, {
                saveCollection()
            })
        }
    }

    interface Callback {
        /**
         * Show response data
         */
        fun onShowResponse(body: String, headers: String, stats: String)

        /**
         * Show request groups and items
         */
        fun onShowCollection(collection: Api, click: (RequestItem) -> Unit, rename: (String, RequestGroup?, RequestItem?) -> Unit, reload: () -> Unit)

        /**
         * Show request data
         */
        fun onShowRequest(url: String, method: String, headers: List<RequestItemHeader>, body: String)

        /**
         * Show ui to select and edit variables
         */
        fun onShowVariablesWindow(collection: Api, save: () -> Unit, remove: (Int) -> Unit, new: () -> Unit)

        /**
         * Show ui to ask to save changes before closing app
         */
        fun onShowSaveDialog(save: () -> Unit)

        /**
         * Show main ui with collection selector
         */
        fun onShowMainApp(collections: List<String>, request: (u: String, m: HttpMethod, b: String, h: Map<String, String>) -> Unit, loadByIndex: (Int) -> Unit, loadByPath: (String) -> Unit, showVariables: () -> Unit, onLoaded: () -> Unit, onClose: () -> Unit, onSave: () -> Unit)

        /**
         * Show ui to enter name
         */
        fun onShowNameDialog(title: String, current: String, save: (String) -> Unit)
    }

    private fun makeRequest(u: String, m: HttpMethod, b: String, h: Map<String, String>) {
        memScoped {
            var url = u
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

                        callback.onShowResponse(body, headers, stats)
                    } else {
                        val stats = "Status: ${response.status.value} ${response.status.description} / Time: ${getTimeMillis() - startTime} ms"

                        callback.onShowResponse("", "", stats)
                    }

                    response.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadCollection(path: String) {
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

                collection = Json.nonstrict.parse(Api.serializer(), content)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                fclose(file)
            }
        }
    }

    private fun showCollection() {
        memScoped {
            callback.onShowCollection(collection, {
                callback.onShowRequest(it.request.url, it.request.method, it.request.headers, it.request.body)
            }, { current: String, requestGroup: RequestGroup?, requestItem: RequestItem? ->
                callback.onShowNameDialog("Rename", current) { name ->
                    requestGroup?.name = name
                    requestItem?.name = name
                    showCollection()
                }
            }, {
                showCollection()
            })
        }
    }

    private fun saveCollection() {
        memScoped {
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
    }

    private fun showVariablesWindow() {
        memScoped {
            if (collection.variables.isEmpty()) {
                showNewVariableSetWindow()
                return
            }
            callback.onShowVariablesWindow(collection, {
                saveCollection()
            }, {
                collection.variables.removeAt(it)
                showVariablesWindow()
            }, {
                showNewVariableSetWindow()
            })
        }
    }

    private fun showNewVariableSetWindow() {
        memScoped {
            callback.onShowNameDialog("New set", "") { name ->
                if (collection.variables.any { it.name == name }) {

                } else {
                    val variablesSet = VariableSet(name, listOf())
                    collection.variables.add(variablesSet)
                    showVariablesWindow()
                }
            }
        }
    }

    private fun getStorageDir(): String {
        memScoped {
            val home = getenv("HOME")?.toKString() ?: ""
            return "$home/Restkid/"
        }
    }

    private fun getCollectionsPath(): String {
        return memScoped { "${getStorageDir()}collections/" }
    }

    private fun getSavedCollections(): MutableList<String> {
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
}