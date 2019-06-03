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
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.*

@Serializable
data class Api(
        var info: ApiInfo,
        @SerialName("item")
        var groups: List<RequestGroup>)

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

val data = listOf<Api>()


val client: HttpClient by lazy {
    HttpClient(Curl)
}

var box: VBox? = null
var uiUrl: TextField? = null
var uiMethod: Combobox? = null
var uiHeaders: TextArea? = null
var uiBody: TextArea? = null
var uiResponse: TextArea? = null

fun main() = appWindow(
        title = "Restkid",
        width = 620,
        height = 300
) {
    hbox {
        vbox {
            hbox {
                button("Import") {
                    action {
                        importCollection()
                    }
                }
                button("Environment") {

                }
                button("Add") {

                }
            }

            separator()

            box = vbox {
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
                        uiBody?.visible = value != 0
                    }
                }
                uiUrl = textfield {
                    stretchy = true
                }
                button("SEND") {

                    action {
                        makeRequest()
                    }
                }
            }

            group("Data") { stretchy = true }.form {
                uiHeaders = textarea {
                    label = "Headers"
                    stretchy = true
                }
                uiBody = textarea {
                    label = "Body"
                    stretchy = true
                }
            }

            group("Response") { stretchy = true }.form {
                uiResponse = textarea {
                    stretchy = true
                }
            }
        }
    }
}

private fun makeRequest() {
    memScoped {
        runBlocking {
            try {
                val url = uiUrl?.value ?: ""

                val call = client.call(url) {
                    method = if (uiMethod?.value == 0) {
                        HttpMethod.Get
                    } else {
                        HttpMethod.Post
                    }
                    uiHeaders?.value?.lines()?.forEach {
                        val dividerIndex = it.indexOf(":")
                        if (dividerIndex != -1) {
                            println("h: $it $dividerIndex")
                            headers.append(it.substring(0, dividerIndex), it.substring(dividerIndex))
                        }
                    }
                }

                val response = call.response.receive<HttpResponse>()


                if (response.status == HttpStatusCode.OK) {
                }

                uiResponse?.value = response.readText()

                response.close()
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
    }
}

private fun importCollection() {
    val importPath = OpenFileDialog() ?: ""
    if (importPath.isNotEmpty()) {
        val file = fopen(importPath, "r")
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

                val obj: Api = Json.nonstrict.parse(Api.serializer(), content)
                println(obj.info.name)

                for (g in obj.groups) {
                    box?.label(g.name)
                    for (item in g.items) {
                        box?.button(item.name) {
                            action {
                                fillRequestData(item.request.url, item.request.method, item.request.headers, item.request.body)
                            }
                        }
                    }
                }
            }
        } finally {
            fclose(file)
        }
    }
}

private fun fillRequestData(url: String, method: String, headers: List<RequestItemHeader>, body: String) {
    println("url: $url")
    uiUrl?.value = url
    uiMethod?.value = if (method == "GET") {
        uiBody?.hide()
        0
    } else {
        uiBody?.show()
        1
    }
    uiHeaders?.value = ""
    for (header in headers) {
        uiHeaders?.append(header.key + ": " + header.value + "\n")
    }
    uiBody?.value = body
}