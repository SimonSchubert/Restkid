import libui.ktx.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
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
import kotlinx.coroutines.*
import kotlin.system.getTimeMillis

@Serializable
data class Api(
        var info: ApiInfo,
        @SerialName("item")
        var groups: List<RequestGroup>,
        var variables: MutableList<VariableSet> = mutableListOf())

@Serializable
data class ApiInfo(
        var name: String = "",
        var description: String = "")

@Serializable
data class RequestGroup(
        var name: String = "",
        var description: String = "",
        @SerialName("item")
        var items: List<RequestItem>
)

@Serializable
data class RequestItem(
        var name: String = "",
        var request: Request = Request()
)

@Serializable
data class VariableSet(
        var name: String = "",
        var variables: List<Variable> = listOf()
)

@Serializable
data class Variable(
        var key: String = "",
        var value: String = ""
)

@Serializable
data class Request(
        // val url: URLUnion? = null,
        var url: String = "",
        var method: String = "",
        var description: String = "",
        var body: String = "",
        @SerialName("header")
        var headers: List<RequestItemHeader> = listOf()
)

@Serializable
data class RequestItemHeader(
        var key: String = "",
        var value: String = "",
        var description: String = ""
)

sealed class URLUnion {
    class StringValue(val u: String) : URLUnion()
    class URLClassValue(val u: URLClass) : URLUnion()
}

data class URLClass(
        val raw: String? = null
)


@Serializable
data class StateSave(
        var selection: Int = 0
)

lateinit var collection: Api

lateinit var box: VBox
lateinit var uiUrl: TextField
lateinit var uiMethod: Combobox
lateinit var uiSend: Button
lateinit var uiHeaders: TextArea
lateinit var uiBody: TextArea
lateinit var uiBodyLabel: Label
lateinit var uiResponseStatus: Label
lateinit var uiResponseBody: TextArea
lateinit var uiResponseHeader: TextArea
var boxChildCount = 0

@ImplicitReflectionSerializer
fun main() = appWindow(
        title = "Restkid",
        width = 620,
        height = 300
) {
    onClose {
        showSaveDialog()
        false
    }
    hbox {
        vbox {
            hbox {
                button("Import") {
                    action {
                        val importPath = OpenFileDialog() ?: ""
                        if (importPath.isNotEmpty()) {
                            importCollection(importPath)
                            fillCollection()
                        }
                    }
                }
                button("Variables") {
                    action {
                        showVariablesWindow()
                    }
                }
                button("Add/Edit") {
                }
            }

            separator()

            val collections = getSavedCollections()
            val collectionComboBox = combobox {
                collections.forEach {
                    item(it)
                }

                action {
                    val path = getCollectionsPath()
                    importCollection("$path/${collections[value]}.json")
                    fillCollection()
                }

            }

            separator()

            box = vbox {
            }

            if (collections.size > 0) {
                collectionComboBox.value = 0
                val path = getCollectionsPath()
                importCollection("$path/${collections[0]}.json")
            }
        }

        separator()
        stretchy = true

        vbox {
            hbox {
                uiMethod = combobox {
                    item("GET")
                    item("POST")

                    action {
                        uiBody.visible = value != 0
                        uiBodyLabel.visible = value != 0
                    }
                }
                uiUrl = textfield {
                    stretchy = true
                }
                uiSend = button("SEND") {
                    action {
                        makeRequest()
                    }
                }
            }

            group("Request") { stretchy = true }.form {
                label("Headers")
                uiHeaders = textarea {
                    stretchy = true
                }
                uiBodyLabel = label("Body")
                uiBody = textarea {
                    stretchy = true
                }
            }

            group("Response") { stretchy = true }.form {
                uiResponseStatus = label("")
                tabpane {
                    stretchy = true
                    page("Body") {
                        uiResponseBody = textarea {
                            stretchy = true
                            readonly = true
                        }
                    }
                    page("Headers") {
                        uiResponseHeader = textarea {
                            stretchy = true
                            readonly = true
                        }
                    }
                }
            }
        }
    }
    fillCollection()
}

private fun showSaveDialog() {
    var alert: Window
    appWindow("Close", 100, 30) {
        alert = this
        vbox {
            label("Do you want to save the changes?")
            hbox {
                button("Cancel") {
                    action {
                        alert.hide()
                    }
                }
                button("No") {
                    action {
                        exit(0)
                    }
                }
                button("Yes") {
                    action {
                        saveCollectionChanges()
                        exit(0)
                    }
                }
            }
        }
        onClose {
            alert.hide()
            false
        }
    }
}

private fun showVariablesWindow() {
    if (collection.variables.isEmpty()) {
        showVariableSetNameWindow()
        return
    }

    lateinit var variableBox: Box
    lateinit var comboBox: Combobox
    var variablesChildCount = 0
    val textfields: MutableList<Pair<TextField, TextField>> = mutableListOf()
    appWindow("Variables", 100, 30) {
        vbox {
            label("Selected set")
            hbox {
                stretchy = true
                comboBox = combobox {
                    stretchy = true
                    collection.variables.forEach {
                        item(it.name)
                    }
                    value = 0
                    action {
                        variablesChildCount = fillVariables(variableBox, textfields, variablesChildCount)
                    }
                }
                button("+") {
                    action {
                        showVariableSetNameWindow()
                    }
                }
                button("-") {
                    action {
                        collection.variables.removeAt(comboBox.value)
                        this@appWindow.hide()
                        showVariablesWindow()
                    }
                }
            }
            separator()
            hbox {
                stretchy = true
                label("Key") {
                    stretchy = true
                }
                label("Value") {
                    stretchy = true
                }
            }
            variableBox = vbox {

            }
            button("Add variable") {
                action {
                    variableBox.hbox {
                        stretchy = true
                        val key = textfield {
                            stretchy = true
                        }
                        val value = textfield {
                            stretchy = true
                        }
                        val pair = Pair(key, value)
                        button("-") {
                            action {
                                textfields.remove(pair)
                                this@hbox.hide()
                            }
                        }
                        textfields.add(pair)
                        variablesChildCount++
                    }
                }
            }
            variablesChildCount = comboBox.fillVariables(variableBox, textfields, variablesChildCount)
        }
        onClose {
            val variables = textfields.map { Variable(it.first.value, it.second.value) }
            collection.variables[comboBox.value].variables = variables
            saveCollectionChanges()
            true
        }
    }
}

fun Combobox.fillVariables(variableBox: Box, textfields: MutableList<Pair<TextField, TextField>>, variablesChildCount: Int): Int {
    for (index in 0 until variablesChildCount) {
        variableBox.delete(0)
    }
    textfields.clear()

    var childCount = 0
    collection.variables.getOrNull(value)?.variables?.forEach {
        variableBox.hbox {
            stretchy = true
            val key = textfield {
                value = it.key
                stretchy = true
            }
            val value = textfield {
                value = it.value
                stretchy = true
            }
            val pair = Pair(key, value)
            button("-") {
                action {
                    textfields.remove(pair)
                    this@hbox.hide()
                }
            }
            textfields.add(pair)
            childCount++
        }
    }
    return childCount
}

private fun showVariableSetNameWindow() {
    appWindow("Create new set", 100, 30) {
        vbox {
            lateinit var textField: TextField
            hbox {
                label("Name:")
                textField = textfield {
                    stretchy = true
                }
            }
            hbox {
                button("Cancel") {
                    this@appWindow.hide()
                }
                button("Save") {
                    action {
                        val name = textField.value
                        if (collection.variables.any { it.name == name }) {

                        } else {
                            val variablesSet = VariableSet(name, listOf())
                            collection.variables.add(variablesSet)

                            this@appWindow.hide()
                            showVariablesWindow()
                        }
                        this@appWindow.hide()
                    }
                }
            }
        }
    }
}

private fun showDeleteSetWindow() {
    appWindow("Delete set", 100, 30) {
        label("Are you sure?")
        hbox {
            button("No") {
                this@appWindow.hide()
            }
            button("Yes") {
                this@appWindow.hide()
            }
        }
    }
}

@ImplicitReflectionSerializer
private fun makeRequest() {
    memScoped {
        runBlocking {
            try {
                uiResponseBody.value = ""
                uiResponseHeader.value = ""

                var url = uiUrl.value
                collection.variables.forEach {
                    it.variables.forEach {
                        url = url.replace("{{${it.key}}}", it.value)
                    }
                }

                val startTime = getTimeMillis()

                val client = HttpClient(Curl)
                val call = client.call(url) {
                    method = if (uiMethod.value == 0) {
                        HttpMethod.Get
                    } else {
                        HttpMethod.Post
                    }

                    uiHeaders.value.lines().forEach {
                        val dividerIndex = it.indexOf(":")
                        if (dividerIndex != -1) {
                            val key = it.substring(0, dividerIndex).trim()
                            val value = it.substring(dividerIndex + 1).trim()
                            println("$key/$value")
                            if (key == "Content-Type") {
                                body = TextContent(uiBody.value, contentType = ContentType.Application.Json)
                            } else {
                                headers.append(key, value)
                            }
                        }
                    }
                }

                val response = call.response.receive<HttpResponse>()


                if (response.status == HttpStatusCode.OK) {

                    val text = response.readText()
                    try {
                        val json = Json.nonstrict.parseJson(text)
                        uiResponseBody.value = Json.indented.stringify(json)
                    } catch (e: Exception) {
                        uiResponseBody.value = text
                    }

                    val headers = response.headers.toMap().toList()
                    uiResponseHeader.value = headers.joinToString(separator = "\n") {
                        it.first + " = " + it.second
                    }
                    val size = (headers.firstOrNull { it.first == "Content-Length" }?.second?.get(0)?.toLong() ?: 0) / 1024f
                    uiResponseStatus.text = "Status: ${response.status.value} ${response.status.description} / Time: ${getTimeMillis()-startTime} ms / Size: $size KB"
                } else {
                    uiResponseStatus.text = "Status: ${response.status.value} ${response.status.description} / Time: ${getTimeMillis()-startTime} ms"
                }

                response.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

private fun importCollection(path: String) {
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
        }
    } finally {
        fclose(file)
    }
}

@ImplicitReflectionSerializer
private fun fillRequestData(url: String, method: String, headers: List<RequestItemHeader>, body: String) {
    println("url: $url")
    uiUrl.value = url
    uiMethod.value = if (method == "GET") {
        uiBody.hide()
        uiBodyLabel.hide()
        0
    } else {
        uiBody.show()
        uiBodyLabel.show()
        1
    }
    uiHeaders.value = ""
    for (header in headers) {
        uiHeaders.append(header.key + ": " + header.value + "\n")
    }
    try {
        val json = Json.nonstrict.parseJson(body)
        uiBody.value = Json.indented.stringify(json)
    } catch (e: Exception) {
        uiBody.value = body
    }
    uiBody.value = body
}

@ImplicitReflectionSerializer
private fun fillCollection() {
    for (index in 0 until boxChildCount) {
        box.delete(0)
    }
    boxChildCount = 0

    println(collection.info.name)
    collection.groups.forEach { group ->
        box.label(group.name)
        boxChildCount++
        for (item in group.items) {
            box.button(item.name) {
                action {
                    fillRequestData(item.request.url, item.request.method, item.request.headers, item.request.body)
                }
            }
            boxChildCount++
            if(boxChildCount == 2) {
                fillRequestData(item.request.url, item.request.method, item.request.headers, item.request.body)
            }
        }
    }
}

private fun saveCurrentState() {
    val path = getStorageDir()
    mkdir(path, S_IRWXU)


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

private fun saveCollectionChanges() {
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