import io.ktor.http.HttpMethod
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonParsingException
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
lateinit var uiNew: Button
var boxChildCount = 0
var hasUnsavedChange = false
var isEditMode = false

@ImplicitReflectionSerializer
var callback = object : AppMaster.Callback {
    override fun onKeepRequestChanges(item: RequestItem) {
        item.request.url = uiUrl.value
        item.request.body = uiBody.value
        item.request.method = if (uiMethod.value == 0) {
            "GET"
        } else {
            "POST"
        }
        item.request.headers = uiHeaders.value.lines().map { it.split(":") }.filter { it.count() > 1 }.map {
            RequestItemHeader(key = it[0].trim(), value = it[1].trim())
        }.toMutableList()
    }

    override fun onShowMainApp(collections: List<String>, request: (u: String, m: HttpMethod, b: String, h: Map<String, String>) -> Unit, loadByIndex: (Int) -> Unit, loadByPath: (String) -> Unit, showVariables: () -> Unit, newCollection: () -> Unit, onLoaded: () -> Unit, onClose: () -> Unit, onSave: () -> Unit) {
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
                        uiNew = button("New") {
                            action {
                                newCollection()
                            }
                        }
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
                            item("PUT")
                            item("DELETE")
                            item("PATCH")
                            item("HEAD")
                            item("OPTIONS")

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
                                uiResponseStatus.text = ""

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

    override fun onShowCollection(collection: Api, show: (RequestItem) -> Unit, rename: (String, RequestGroup?, RequestItem?) -> Unit, add: (RequestGroup?) -> Unit, reload: () -> Unit) {
        for (index in 0 until boxChildCount) {
            box.delete(0)
        }
        boxChildCount = 0

        println(collection.info.name)
        collection.groups.forEachIndexed { index, group ->
            val uiGroup = box.vbox {
            }
            uiGroup.hbox {
                label(group.name) {
                    stretchy = true
                }
                if (isEditMode) {
                    button(CharSymbol.DELETE) {
                        action {
                            collection.groups.remove(group)
                            uiGroup.hide()
                        }
                    }
                    button(CharSymbol.EDIT) {
                        action {
                            rename(group.name, group, null)
                        }
                    }
                    button(CharSymbol.UP) {
                        action {
                            collection.groups.remove(group)
                            collection.groups.add(index - 1, group)
                            reload()
                        }
                        enabled = index > 0
                    }
                    button(CharSymbol.DOWN) {
                        action {
                            collection.groups.remove(group)
                            collection.groups.add(index + 1, group)
                            reload()
                        }
                        enabled = index < collection.groups.count() - 1
                    }
                }
            }
            group.items.forEachIndexed { index, item ->
                uiGroup.hbox {
                    stretchy = false
                    button(item.name) {
                        stretchy = true
                        action {
                            show(item)
                        }
                    }
                    if (isEditMode) {
                        button(CharSymbol.DELETE) {
                            action {
                                group.items.remove(item)
                                this@hbox.hide()
                            }
                        }
                        button(CharSymbol.EDIT) {
                            action {
                                rename(item.name, null, item)
                            }
                        }
                        button(CharSymbol.UP) {
                            action {
                                group.items.remove(item)
                                group.items.add(index - 1, item)
                                reload()
                            }
                            enabled = index > 0
                        }
                        button(CharSymbol.DOWN) {
                            action {
                                group.items.remove(item)
                                group.items.add(index + 1, item)
                                reload()
                            }
                            enabled = index < group.items.count() - 1
                        }
                    }
                }
                if (boxChildCount == 0 && index == 0) {
                    show(item)
                }
            }
            if (isEditMode) {
                uiGroup.button(CharSymbol.PLUS) {
                    action {
                        add(group)
                    }
                }
            }

            boxChildCount++
        }
        if (isEditMode) {
            box.button("＋ group") {
                action {
                    add(null)
                }
            }
            boxChildCount++
        }

        // workaround for last button vertical centered bug
        box.vbox {

        }
        boxChildCount++
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
            when (method) {
                "POST" -> 1
                "PUT" -> 2
                "DELETE" -> 3
                "PATCH" -> 4
                "HEAD" -> 5
                else -> 6
            }
        }
        uiHeaders.value = ""
        for (header in headers) {
            uiHeaders.append(header.key + ": " + header.value + "\n")
        }
        try {
            val json = Json.nonstrict.parseJson(body)
            uiBody.value = Json.indented.stringify(json)
        } catch (e: JsonParsingException) {
            uiBody.value = body
        }
        uiBody.value = body
        uiResponseBody.value = ""
        uiResponseHeader.value = ""
        uiResponseStatus.text = ""
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

    override fun onShowNameDialog(title: String, current: String, save: (String) -> Unit) {
        appWindow(title, 100, 30) {
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
                            this@appWindow.hide()
                            save(uiText.value)
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
                    button("＋") {
                        action {
                            this@appWindow.hide()
                            new()
                        }
                    }
                    button("⌫") {
                        action {
                            this@appWindow.hide()
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
                button("＋") {
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
                            button("⌫") {
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
                this@appWindow.hide()
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
    uiNew.enabled = !isEditMode
    uiCancel.visible = isEditMode
    uiEdit.text = if (isEditMode) {
        "Save"
    } else {
        "Edit"
    }
}

private fun Combobox.fillVariables(collection: Api, variableBox: Box, textFields: MutableList<Pair<TextField, TextField>>, variablesChildCount: Int): Int {
    for (index in 0 until variablesChildCount) {
        variableBox.delete(0)
    }
    textFields.clear()

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
            button("⌫") {
                action {
                    textFields.remove(pair)
                    this@hbox.hide()
                }
            }
            textFields.add(pair)
            childCount++
        }
    }
    return childCount
}