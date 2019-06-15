import io.ktor.http.HttpMethod
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.stringify
import libui.ktx.*
import models.*
import platform.posix.exit

lateinit var box: VBox
lateinit var uiUrl: TextField
lateinit var uiMethod: Combobox
lateinit var uiSend: Button
lateinit var uiImport: Button
lateinit var uiVariables: Button
lateinit var uiEdit: Button
lateinit var uiHeaders: TextArea
lateinit var uiBody: TextArea
lateinit var uiBodyLabel: Label
lateinit var uiResponseStatus: Label
lateinit var uiResponseBody: TextArea
lateinit var uiResponseHeader: TextArea
lateinit var uiCollection: Combobox
lateinit var uiCancel: Button
var boxChildCount = 0
var hasUnsavedChange = false
var isEditMode = false

@ImplicitReflectionSerializer
var callback = object : AppMaster.Callback {

    override fun onShowMainApp(collections: List<String>, request: (u: String, m: HttpMethod, b: String, h: Map<String, String>) -> Unit, loadByIndex: (Int) -> Unit, loadByPath: (String) -> Unit, showVariables: () -> Unit, onLoaded: () -> Unit, onClose: () -> Unit, onSave: () -> Unit) {
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
                        uiImport = button("Import") {
                            action {
                                val importPath = OpenFileDialog() ?: ""
                                if (importPath.isNotEmpty()) {
                                    loadByPath(importPath)
                                }
                            }
                        }
                        uiVariables = button("Variables") {
                            action {
                                showVariables()
                            }
                        }
                        uiEdit = button("Edit") {
                            action {
                                if (isEditMode) {
                                    onSave()
                                }

                                toggleEditMode()
                                onLoaded()
                            }
                        }
                        uiCancel = button("Cancel") {
                            action {
                                toggleEditMode()
                                onLoaded()
                            }
                            visible = false
                        }
                    }

                    separator()

                    uiCollection = combobox {
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
                                hasUnsavedChange = true
                            }
                        }
                        uiUrl = textfield {
                            stretchy = true
                            action {
                                hasUnsavedChange = true
                            }
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

    override fun onShowCollection(collection: Api, click: (RequestItem) -> Unit, rename: (String, RequestGroup?, RequestItem?) -> Unit) {
        for (index in 0 until boxChildCount) {
            box.delete(0)
        }
        boxChildCount = 0

        println(collection.info.name)
        collection.groups.forEach { group ->
            val uiGroup = box.vbox {}
            uiGroup.hbox {
                label(group.name)
                if (isEditMode) {
                    button("⌫") {
                        action {
                            collection.groups.remove(group)
                            uiGroup.hide()
                        }
                    }
                    button("✎") {
                        action {
                            rename(group.name, group, null)
                        }
                    }
                }
            }
            group.items.forEachIndexed { index, item ->
                uiGroup.hbox {
                    button(item.name) {
                        stretchy = true
                        action {
                            click(item)
                        }
                    }
                    if (isEditMode) {
                        button("⌫") {
                            action {
                                group.items.remove(item)
                                this@hbox.hide()
                            }
                        }
                        button("✎") {
                            action {
                                rename(item.name, null, item)
                            }
                        }
                    }
                }
                if (boxChildCount == 0 && index == 0) {
                    click(item)
                }
            }

            boxChildCount++
        }
    }

    override fun onShowRequest(url: String, method: String, headers: List<RequestItemHeader>, body: String) {
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
        uiResponseBody.value = ""
        uiResponseHeader.value = ""
    }

    override fun onShowResponse(body: String, headers: String, stats: String) {
        uiResponseBody.value = body
        uiResponseHeader.value = headers
        uiResponseStatus.text = stats
    }

    override fun onShowSaveDialog(save: () -> Unit) {
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
        }
    }

    override fun onShowNewVariableSetWindow(collection: Api, save: (String) -> Unit) {
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

    override fun onShowNameDialog(current: String, save: (String) -> Unit) {
        appWindow("Rename", 100, 30) {
            vbox {
                val uiText = textfield {
                    value = current
                }
                hbox {
                    button("Cancel") {
                        action {
                            this@appWindow.hide()
                        }
                    }
                    button("Save") {
                        action {
                            save(uiText.value)
                            this@appWindow.hide()
                        }
                    }
                }
            }
            onClose {
                this@appWindow.hide()
                true
            }
        }
    }

    override fun onShowVariablesWindow(collection: Api, save: () -> Unit, remove: (Int) -> Unit, new: () -> Unit) {
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

private fun toggleEditMode() {
    isEditMode = !isEditMode
    uiVariables.enabled = !isEditMode
    uiImport.enabled = !isEditMode
    uiSend.enabled = !isEditMode
    uiMethod.enabled = !isEditMode
    uiBody.enabled = !isEditMode
    uiHeaders.enabled = !isEditMode
    uiResponseBody.enabled = !isEditMode
    uiResponseHeader.enabled = !isEditMode
    uiCollection.enabled = !isEditMode
    uiUrl.enabled = !isEditMode
    uiCancel.visible = isEditMode
    uiEdit.text = if (isEditMode) {
        "Save"
    } else {
        "Edit"
    }
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