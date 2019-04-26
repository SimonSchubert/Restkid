import libui.ktx.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
// import io.ktor.client.HttpClient
// import io.ktor.client.call.call
// import io.ktor.client.engine.curl.Curl
// import kotlinx.coroutines.GlobalScope
// import kotlinx.coroutines.launch

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

fun main() = appWindow(
        title = "Restkid",
        width = 620,
        height = 300
) {
    hbox {
        var importPath: String
        var box: VBox? = null
        var uiUrl: TextField? = null
        var uiMethod: Combobox? = null
        var uiHeaders: TextArea? = null
        var uiBody: TextArea? = null

        vbox {
            hbox {
                button("Import") {
                    action {
                        importPath = OpenFileDialog() ?: ""
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
                                            val url = item.request.url
                                            val method = item.request.method
                                            println("url: $url $method")
                                            box?.button(item.name) {
                                                action {
                                                    println("url: $url")
                                                    uiUrl?.value = url
                                                    uiMethod?.value = if (method == "GET") {
                                                        0
                                                    } else {
                                                        1
                                                    }
                                                    uiHeaders?.value = ""
                                                    for (header in item.request.headers) {
                                                        uiHeaders?.append(header.key + ": " + header.value + "\n")
                                                    }
                                                    uiBody?.value = item.request.body
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
                }
                button("Variables") {

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
                }
                uiUrl = textfield() {
                    stretchy = true
                }
                button("SEND") {

                    memScoped {
                        /*
                        val client = HttpClient(Curl)

                        val req1 = client.call("http://example.com").response.content.readByte()
                        // val bytes1 = req1.await()
                        printf("bytes: $req1")
                        client.close()
                        */
                    }
                    // */

                    /*
                    val req1 = async { client1.call("http://example.com").response.readBytes() }
                    val bytes1 = req1.await()
                    printf("bytes: $bytes1")
                    client1.close()
                    */

                    /*
                    val curl = curl_easy_init()
                    printf("curl: " (curl != null))
                    if (curl != null) {
                        curl_easy_setopt(curl, CURLOPT_URL, "http://example.com")
                        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L)
                        val res = curl_easy_perform(curl)
                        if (res != CURLE_OK) {
                            println("curl_easy_perform() failed ${curl_easy_strerror(res)?.toKString()}")
                        }
                        curl_easy_cleanup(curl)
                    }
                    */
                }
            }

            /*
            tabpane {
                page("Headers") {
                    form {
                    }
                }
                page("Body") {
                }
            }
            */

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
                textarea {
                    stretchy = true
                }
            }
        }
    }
}