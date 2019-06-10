import io.ktor.http.HttpMethod
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.stringify
import libui.ktx.*
import models.Api
import models.RequestItem
import models.RequestItemHeader
import models.Variable
import platform.posix.exit

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
lateinit var collectionComboBox: Combobox
var boxChildCount = 0
var hasUnsavedChange = false

@ImplicitReflectionSerializer
var callback = object : AppMaster.Callback {
    override fun showMainApp(collections: List<String>, request: (u: String, m: HttpMethod, b: String, h: Map<String, String>) -> Unit, loadByIndex: (Int) -> Unit, loadByPath: (String) -> Unit, showVariables: () -> Unit, onLoaded: () -> Unit, onClose: () -> Unit) {
        appWindow(
                title = "Restkid",
                width = 620,
                height = 300
        ) {
            onClose {
                if (hasUnsavedChange) {
                    onClose()
                    false
                } else {
                    exit(0)
                    true
                }
            }
            hbox {
                vbox {
                    hbox {
                        button("Import") {
                            action {
                                val importPath = OpenFileDialog() ?: ""
                                if (importPath.isNotEmpty()) {
                                    loadByPath(importPath)
                                }
                            }
                        }
                        button("Variables") {
                            action {
                                showVariables()
                            }
                        }
                        button("Add/Edit") {
                        }
                    }

                    separator()

                    collectionComboBox = combobox {
                        collections.forEach {
                            item(it)
                        }

                        action {
                            loadByIndex(value)
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
                                uiBody.visible = value != 0
                                uiBodyLabel.visible = value != 0
                            }
                        }
                        uiUrl = textfield {
                            stretchy = true
                        }
                        uiSend = button("SEND") {
                            action {
                                uiResponseBody.value = ""
                                uiResponseHeader.value = ""

                                val url = uiUrl.value
                                val method = if (uiMethod.value == 0) {
                                    HttpMethod.Get
                                } else {
                                    HttpMethod.Post
                                }

                                val headers = uiHeaders.value.lines().filter { it.indexOf(":") != -1 }.map {
                                    val dividerIndex = it.indexOf(":")
                                    val key = it.substring(0, dividerIndex).trim()
                                    val value = it.substring(dividerIndex + 1).trim()
                                    key to value
                                }.toMap()

                                request(url, method, uiBody.value, headers)
                            }
                        }
                    }

                    group("Request") { stretchy = true }.form {
                        label("Headers")
                        uiHeaders = textarea {
                            stretchy = true
                            action {
                                hasUnsavedChange = true
                            }
                        }
                        uiBodyLabel = label("Body")
                        uiBody = textarea {
                            stretchy = true
                            action {
                                hasUnsavedChange = true
                            }
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

            onLoaded()
        }
    }

    override fun showCollection(collection: Api, click: (RequestItem) -> Unit) {
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
                        click(item)
                    }
                }
                boxChildCount++
                if (boxChildCount == 2) {
                    click(item)
                }
            }
            if (boxChildCount > 10) {
                return
            }
        }
    }

    override fun showRequest(url: String, method: String, headers: List<RequestItemHeader>, body: String) {
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

    override fun showResponse(body: String, headers: String, stats: String) {
        uiResponseBody.value = body
        uiResponseHeader.value = headers
        uiResponseStatus.text = stats
    }

    override fun showSaveDialog(save: () -> Unit) {
        appWindow("Close", 100, 30) {
            vbox {
                label("Do you want to save the changes?")
                hbox {
                    button("Cancel") {
                        action {
                            this@appWindow.hide()
                        }
                    }
                    button("No") {
                        action {
                            exit(0)
                        }
                    }
                    button("Yes") {
                        action {
                            save()
                            exit(0)
                        }
                    }
                }
            }
            onClose {
                this@appWindow.hide()
                false
            }
        }
    }

    override fun showNewVariableSetWindow(collection: Api, save: (String) -> Unit) {
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
                            save(name)
                            this@appWindow.hide()
                        }
                    }
                }
            }
        }
    }

    override fun showVariablesWindow(collection: Api, save: () -> Unit, remove: (Int) -> Unit, new: () -> Unit) {
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
                            variablesChildCount = fillVariables(collection, variableBox, textfields, variablesChildCount)
                        }
                    }
                    button("+") {
                        action {
                            new()
                        }
                    }
                    button("-") {
                        action {
                            remove(comboBox.value)
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
                variablesChildCount = comboBox.fillVariables(collection, variableBox, textfields, variablesChildCount)
            }
            onClose {
                val variables = textfields.map { Variable(it.first.value, it.second.value) }
                collection.variables[comboBox.value].variables = variables
                save()
                true
            }
        }
    }
}

@ImplicitReflectionSerializer
var appMaster = AppMaster(callback)

@ImplicitReflectionSerializer
fun main() {
    appMaster.start()
}

fun Combobox.fillVariables(collection: Api, variableBox: Box, textfields: MutableList<Pair<TextField, TextField>>, variablesChildCount: Int): Int {
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