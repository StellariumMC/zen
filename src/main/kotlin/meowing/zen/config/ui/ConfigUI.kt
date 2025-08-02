package meowing.zen.config.ui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.CramSiblingConstraint
import gg.essential.elementa.constraints.RelativeConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.universal.UKeyboard
import meowing.zen.config.ui.constraint.ChildHeightConstraint
import meowing.zen.config.ui.core.ConfigTheme
import meowing.zen.config.ui.core.ConfigValidator
import meowing.zen.config.ui.core.ElementFactory
import meowing.zen.config.ui.elements.ColorPicker
import meowing.zen.config.ui.elements.Dropdown
import meowing.zen.config.ui.elements.TextInput
import meowing.zen.config.ui.types.*
import meowing.zen.utils.ChatUtils
import meowing.zen.utils.DataUtils
import meowing.zen.utils.Utils.createBlock
import meowing.zen.utils.Utils.toColorFromMap
import java.awt.Color

typealias ConfigData = Map<String, Any>

/*
 * Inspired by NoammAddons' Config GUI Design
 * https://github.com/Noamm9/NoammAddons
 */
class ConfigUI(configFileName: String = "config") : WindowScreen(ElementaVersion.V10, true, false, true, 2) {
    private val dataUtils = DataUtils(configFileName, mutableMapOf<String, Any>())
    private val config: MutableMap<String, Any> = dataUtils.getData()
    private val validator = ConfigValidator()
    private val theme = ConfigTheme()
    private val factory = ElementFactory(theme)

    private val categories = mutableListOf<ConfigCategory>()
    private val sections = mutableMapOf<String, MutableList<ConfigSection>>()
    private val subcategories = mutableMapOf<String, MutableList<ConfigSubcategory>>()
    private val elementContainers = mutableMapOf<String, UIComponent>()
    private val elementRefs = mutableMapOf<String, ConfigElement>()
    private val configListeners = mutableMapOf<String, MutableList<(Any) -> Unit>>()
    private val sectionToggleElements = mutableMapOf<String, String>()
    private val sectionToggleRefs = mutableMapOf<String, UIComponent>()
    private val gearImages = mutableMapOf<String, UIImage>()

    private var activeCategory: String? = null
    private var activeSection: String? = null
    private var searchQuery: String = ""
    private var filteredCategories = mutableListOf<ConfigCategory>()
    private var filteredSections = mutableMapOf<String, MutableList<ConfigSection>>()

    private lateinit var searchInput: TextInput
    private lateinit var categoryScroll: ScrollComponent
    private lateinit var sectionScroll: ScrollComponent
    private lateinit var elementScroll: ScrollComponent

    init {
        createGUI()
    }

    private fun createGUI() {
        val border = createBlock(4f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 70.percent()
            height = 65.percent()
        }.setColor(theme.accent2) childOf window

        val main = createBlock(4f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 100.percent() - 2.pixels()
            height = 100.percent() - 2.pixels()
        }.setColor(theme.bg) childOf border

        createPanels(main)
    }

    private fun createPanels(parent: UIComponent) {
        val categoryPanel = createBlock(2f).constrain {
            x = 0.percent()
            y = 0.percent()
            width = 15.percent()
            height = 100.percent()
        }.setColor(Color(0,0,0,0)) childOf parent

        val titleBox = createBlock(2f).constrain {
            x = 0.percent()
            y = 0.percent()
            width = 100.percent()
            height = 30.pixels()
        }.setColor(Color(0,0,0,0)) childOf categoryPanel

        UIText("§lZen").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 2.pixels()
        }.setColor(theme.accent) childOf titleBox

        createBlock(0f).constrain {
            x = 15.percent()
            y = 0.percent()
            width = 1.pixels()
            height = 100.percent()
        }.setColor(theme.accent2.darker()) childOf parent

        categoryScroll = ScrollComponent().constrain {
            x = 2.percent()
            y = 2.percent() + 24.pixels
            width = 96.percent()
            height = 96.percent()
        } childOf categoryPanel

        createHudEditorButton(categoryPanel)

        val searchBarHeader = createBlock(0f).constrain {
            x = 15.percent() + 1.pixels()
            y = 4.pixels
            width = 30.percent() - 1.pixels()
            height = 24.pixels
        }.setColor(theme.panel) childOf parent

        searchInput = (TextInput(placeholder = "Type to search...").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 96.percent()
            height = 20.pixels()
        }.setColor(theme.bg) childOf searchBarHeader) as TextInput

        searchInput.grabFocus()

        searchInput.onKeyInput { input ->
            val newQuery = input.lowercase().trim()
            if (newQuery != searchQuery) {
                searchQuery = newQuery
                performSearch()
            }
        }

        val sectionPanel = createBlock(2f).constrain {
            x = 15.percent() + 1.pixels()
            y = 28.pixels
            width = 30.percent() - 1.pixels()
            height = 100.percent() - 28.pixels
        }.setColor(theme.panel) childOf parent

        createBlock(0f).constrain {
            x = 44.9.percent()
            y = 0.percent()
            width = 1.pixels()
            height = 100.percent()
        }.setColor(theme.accent2.darker()) childOf parent

        sectionScroll = ScrollComponent().constrain {
            x = 2.percent()
            y = 2.pixels
            width = 96.percent()
            height = 100.percent() - 2.pixels
        } childOf sectionPanel

        val elementPanel = createBlock(2f).constrain {
            x = 44.9.percent() + 1.pixels()
            y = 0.percent()
            width = 55.percent()
            height = 100.percent()
        }.setColor(Color(0,0,0,0)) childOf parent

        elementScroll = ScrollComponent().constrain {
            x = 2.percent()
            y = 2.percent()
            width = 96.percent()
            height = 96.percent()
        } childOf elementPanel
    }

    private fun createHudEditorButton(categoryPanel: UIComponent) {
        val editLocationsBorder = createBlock(3f).constrain {
            x = CenterConstraint()
            y = 2.pixels(true)
            width = 95.percent()
            height = 24.pixels()
        }.setColor(theme.accent2) childOf categoryPanel

        val editLocations = createBlock(3f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 100.percent() - 2.pixels()
            height = 100.percent() - 2.pixels()
        }.setColor(theme.bg) childOf editLocationsBorder

        UIText("HUD Editor").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 0.8.pixels()
        }.setColor(theme.accent2) childOf editLocations

        editLocations.onMouseEnter {
            animate { setColorAnimation(Animations.OUT_QUAD, 0.2f, theme.bg.withAlpha(200).toConstraint()) }
        }.onMouseLeave {
            animate { setColorAnimation(Animations.OUT_QUAD, 0.2f, theme.bg.toConstraint()) }
        }.onMouseClick {
            ChatUtils.clientCommand("zen hud")
        }
    }

    private fun performSearch() {
        if (searchQuery.isEmpty()) {
            filteredCategories.clear()
            filteredCategories.addAll(categories)
            filteredSections.clear()
            sections.forEach { (key, value) ->
                filteredSections[key] = value.toMutableList()
            }
        } else {
            filterContent()
        }

        updateCategories()

        if (filteredCategories.isNotEmpty() && (activeCategory == null || !filteredCategories.any { it.name == activeCategory })) {
            activeCategory = filteredCategories.first().name
            activeSection = null
        }

        activeCategory?.let { categoryName ->
            val availableSections = filteredSections[categoryName]
            if (availableSections?.isNotEmpty() == true && (activeSection == null || !availableSections.any { it.name == activeSection })) {
                activeSection = availableSections.first().name
            } else if (availableSections?.isEmpty() == true) {
                activeSection = null
            }
        }

        updateSections()
        updateElements()
    }

    private fun createCategory(text: String, isActive: Boolean, onClick: () -> Unit): UIComponent {
        val item = createBlock(3f).constrain {
            x = (-100).percent()
            y = CramSiblingConstraint(2f)
            width = 95.percent()
            height = 24.pixels()
        }.setColor(if (isActive) theme.accent.withAlpha(60) else Color(0,0,0,0))

        if (!isActive) {
            item.onMouseEnter {
                animate { setColorAnimation(Animations.OUT_QUAD, 0.2f, theme.accent2.withAlpha(30).toConstraint()) }
            }.onMouseLeave {
                animate { setColorAnimation(Animations.OUT_QUAD, 0.2f, Color(0,0,0,0).toConstraint()) }
            }.onMouseClick { onClick() }
        }

        UIText(text).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 0.9.pixels()
        }.setColor(if (isActive) theme.accent else theme.accent2) childOf item

        return item
    }

    private fun createSectionWithToggle(section: ConfigSection, isActive: Boolean, onClick: () -> Unit): UIComponent {
        val sectionKey = "${activeCategory}-${section.name}"
        val toggleConfigKey = sectionToggleElements[sectionKey]
        val hasElements = subcategories[sectionKey]?.flatMap { it.elements }?.any { it.configKey != toggleConfigKey } == true

        val item = createBlock(3f).constrain {
            x = 0.percent()
            y = CramSiblingConstraint(2f)
            width = 100.percent()
            height = 24.pixels()
        }.setColor(if (isActive) theme.accent.withAlpha(60) else Color(0,0,0,0))

        if (!isActive) {
            item.onMouseEnter {
                animate { setColorAnimation(Animations.OUT_QUAD, 0.15f, theme.accent2.withAlpha(30).toConstraint()) }
            }.onMouseLeave {
                animate { setColorAnimation(Animations.OUT_QUAD, 0.15f, Color(0,0,0,0).toConstraint()) }
            }.onMouseClick { if (hasElements) onClick() }
        }

        UIText(section.name).constrain {
            x = 8.pixels()
            y = CenterConstraint()
            textScale = 0.9.pixels()
        }.setColor(theme.accent2) childOf item

        if (hasElements) {
            gearImages.getOrPut(sectionKey) { UIImage.ofResource("/assets/zen/logos/gear.png") }.constrain {
                x = if(toggleConfigKey != null) 28.pixels(true) else 4.pixels(true)
                y = CenterConstraint()
                width = 14.pixels()
                height = 14.pixels()
            } childOf item
        }

        toggleConfigKey?.let { key ->
            subcategories[sectionKey]?.flatMap { it.elements }?.find { it.configKey == key }?.takeIf { it.type is ElementType.Switch }?.let { toggleElement ->
                val currentValue = getConfigValue(key) as? Boolean ?: (toggleElement.type as ElementType.Switch).default
                val switchElement = ConfigElement(key, toggleElement.title ?: "", ElementType.Switch(currentValue))
                factory.createSwitch(switchElement, config, 2f, 35f) { updateConfig(key, it) }.constrain {
                    x = RelativeConstraint(1f) - 30.pixels()
                    y = CenterConstraint()
                    width = 20.pixels()
                    height = 10.pixels()
                }.onMouseClick { it.stopPropagation() }.also {
                    sectionToggleRefs[sectionKey] = it
                } childOf item
            }
        }
        return item
    }

    private fun updateCategories() {
        categoryScroll.clearChildren()
        UIContainer().constrain {
            width = 100.percent()
            height = ChildHeightConstraint(2f)
        }.also { container ->
            filteredCategories.forEach { category ->
                createCategory(category.name, category.name == activeCategory) {
                    switchCategory(category.name)
                }.setX(CenterConstraint()) childOf container
            }
        } childOf categoryScroll
    }

    private fun switchCategory(categoryName: String) {
        if (activeCategory == categoryName) return
        activeCategory = categoryName
        activeSection = null
        updateCategories()
        updateSections()
        elementScroll.clearChildren()
        closePopups()

        val availableSections = filteredSections[categoryName]
        availableSections?.firstOrNull()?.let { firstSection ->
            activeSection = firstSection.name
            updateSections()
            updateElements()
        }
    }

    private fun updateSections() {
        sectionScroll.clearChildren()
        activeCategory?.let { categoryName ->
            val sectionsToShow = filteredSections[categoryName]

            val container = UIContainer().constrain {
                width = 100.percent()
                height = ChildHeightConstraint(2f)
            }

            sectionsToShow?.forEach { section ->
                createSectionWithToggle(section, section.name == activeSection) {
                    switchSection(section.name)
                } childOf container
            }

            container childOf sectionScroll
        }
    }

    private fun updateElements() {
        elementScroll.clearChildren()
        activeSection?.let { sectionName ->
            val sectionKey = "${activeCategory}-${sectionName}"
            val toggleConfigKey = sectionToggleElements[sectionKey]

            subcategories[sectionKey]?.let { subcatList ->
                UIContainer().constrain {
                    width = 100.percent()
                    height = ChildHeightConstraint(6f)
                }.also { container ->
                    subcatList.forEach { subcat ->
                        val elementsToShow = subcat.elements.filter { it.configKey != toggleConfigKey }

                        if (elementsToShow.isNotEmpty()) {
                            createSubcategoryHeader(container, subcat.name)
                            elementsToShow.forEach { element ->
                                createElementUI(container, element)
                            }
                        }
                    }
                } childOf elementScroll
            }
        }
    }

    private fun filterContent() {
        filteredCategories.clear()
        filteredSections.clear()

        categories.forEach { category ->
            val categoryMatches = category.name.lowercase().contains(searchQuery)
            val matchingSections = mutableListOf<ConfigSection>()

            sections[category.name]?.forEach { section ->
                val sectionMatches = section.name.lowercase().contains(searchQuery)

                if (sectionMatches || categoryMatches) {
                    matchingSections.add(section)
                }
            }

            if (matchingSections.isNotEmpty()) {
                filteredCategories.add(category)
                filteredSections[category.name] = matchingSections
            }
        }
    }

    private fun createSubcategoryHeader(parent: UIComponent, name: String) {
        val dividerContainer = UIContainer().constrain {
            x = 0.percent()
            y = CramSiblingConstraint(8f)
            width = 100.percent()
            height = 16.pixels()
        } childOf parent

        createBlock(0f).constrain {
            x = 0.percent()
            y = CenterConstraint()
            width = 35.percent()
            height = 1.pixels()
        }.setColor(theme.accent2) childOf dividerContainer

        UIText(name).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 0.8.pixels()
        }.setColor(theme.accent) childOf dividerContainer

        createBlock(0f).constrain {
            x = 65.percent()
            y = CenterConstraint()
            width = 35.percent()
            height = 1.pixels()
        }.setColor(theme.accent2) childOf dividerContainer
    }

    private fun createElementUI(parent: UIComponent, element: ConfigElement) {
        val isFullWidth = element.type is ElementType.Slider ||
                element.type is ElementType.TextInput ||
                element.type is ElementType.TextParagraph ||
                element.type is ElementType.Button ||
                element.type is ElementType.Dropdown

        val elementHeight = when {
            isFullWidth -> 48.pixels()
            else -> 28.pixels()
        }

        val elementContainer = UIContainer().constrain {
            x = 0.percent()
            y = CramSiblingConstraint(6f)
            width = 100.percent()
            height = elementHeight
        } childOf parent

        val card = createBlock(3f).constrain {
            x = 2.percent()
            y = 0.percent()
            width = 96.percent + 1.pixels
            height = 100.percent + 1.pixels
        }.setColor(theme.accent) childOf elementContainer

        val innerCard = createBlock(3f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 100.percent - 1.pixels
            height = 100.percent - 1.pixels
        }.setColor(theme.bg) childOf card

        element.title?.let { title ->
            if (element.type !is ElementType.TextParagraph) {
                UIText(title).constrain {
                    x = 8.pixels()
                    y = if (isFullWidth) 8.pixels() else CenterConstraint()
                    textScale = 0.8.pixels()
                }.setColor(theme.accent) childOf innerCard
            }
        }

        val widget = createElementWidget(element)
        widget.constrain {
            x = if (isFullWidth) 8.pixels() else RelativeConstraint(1f) - 56.pixels()
            y = if (isFullWidth) 22.pixels() else CenterConstraint()
            width = if (isFullWidth) RelativeConstraint(1f) - 16.pixels() else 50.pixels()
            height = if (isFullWidth) 18.pixels() else 16.pixels()
        } childOf card

        widget.onMouseClick { it ->
            it.stopPropagation()
            Dropdown.closeAllDropdowns()
        }

        elementContainers[element.configKey] = elementContainer
        elementRefs[element.configKey] = element
        updateElementVisibility(element.configKey)
    }

    private fun createElementWidget(element: ConfigElement): UIComponent {
        return when (element.type) {
            is ElementType.Button -> factory.createButton(element, config, this)
            is ElementType.Switch -> factory.createSwitch(element, config) { updateConfig(element.configKey, it) }
            is ElementType.Slider -> factory.createSlider(element, config) { updateConfig(element.configKey, it) }
            is ElementType.Dropdown -> factory.createDropdown(element, config) { updateConfig(element.configKey, it) }
            is ElementType.TextInput -> factory.createTextInput(element, config) { updateConfig(element.configKey, it) }
            is ElementType.TextParagraph -> factory.createTextParagraph(element)
            is ElementType.ColorPicker -> factory.createColorPicker(element, config) { updateConfig(element.configKey, it) }
            is ElementType.Keybind -> factory.createKeybind(element, config) { updateConfig(element.configKey, it) }
        }
    }

    private fun updateConfig(configKey: String, newValue: Any) {
        val validatedValue = validator.validate(configKey, newValue) ?: return

        val serializedValue = when (validatedValue) {
            is Color -> mapOf(
                "r" to validatedValue.red,
                "g" to validatedValue.green,
                "b" to validatedValue.blue,
                "a" to validatedValue.alpha
            )
            else -> validatedValue
        }

        config[configKey] = serializedValue
        dataUtils.setData(config)
        updateElementVisibilities()
        configListeners[configKey]?.forEach { it(validatedValue) }
        updateSectionToggles()
    }

    private fun updateSectionToggles() {
        sectionToggleRefs.forEach { (sectionKey, toggleRef) ->
            sectionToggleElements[sectionKey]?.let { toggleConfigKey ->
                val newValue = getConfigValue(toggleConfigKey) as? Boolean ?: false
                factory.updateSwitchValue(toggleRef, newValue)
            }
        }
    }

    private fun updateElementVisibilities() {
        elementRefs.keys.forEach { updateElementVisibility(it) }
    }

    private fun updateElementVisibility(configKey: String) {
        val container = elementContainers[configKey] ?: return
        val element = elementRefs[configKey] ?: return
        val visible = element.shouldShow(config)
        if (visible) container.unhide(true) else container.hide()
    }

    private fun switchSection(sectionName: String) {
        if (activeSection == sectionName) return
        activeSection = sectionName
        updateSections()
        updateElements()
        closePopups()
    }

    private fun getDefaultValue(type: ElementType?): Any? = when (type) {
        is ElementType.Switch -> type.default
        is ElementType.Slider -> type.default
        is ElementType.Dropdown -> type.default
        is ElementType.TextInput -> type.default
        is ElementType.ColorPicker -> type.default
        is ElementType.Keybind -> type.default
        else -> null
    }

    private fun closePopups() {
        if (ColorPicker.isPickerOpen) {
            ColorPicker.closePicker()
            return
        }
        if (Dropdown.openDropdown != null) Dropdown.closeAllDropdowns()
    }

    override fun onKeyPressed(keyCode: Int, typedChar: Char, modifiers: UKeyboard.Modifiers?) {
        if (keyCode == 1) {
            if (ColorPicker.isPickerOpen) {
                ColorPicker.closePicker()
                return
            }
            if (Dropdown.openDropdown != null) {
                Dropdown.closeAllDropdowns()
                return
            }

            super.onKeyPressed(keyCode, typedChar, modifiers)
            return
        }
        super.onKeyPressed(keyCode, typedChar, modifiers)
    }

    override fun onScreenClose() {
        super.onScreenClose()
        saveConfig()
    }

    fun addElement(categoryName: String, sectionName: String, element: ConfigElement, isSectionToggle: Boolean = false) =
        addElement(categoryName, sectionName, "Backwards Config", element, isSectionToggle)

    fun addElement(categoryName: String, sectionName: String, subcategoryName: String, element: ConfigElement, isSectionToggle: Boolean = false): ConfigUI {
        val isFirstCategory = categories.isEmpty()

        categories.find { it.name == categoryName } ?: ConfigCategory(categoryName).also { categories.add(it) }

        val sectionList = sections.getOrPut(categoryName) { mutableListOf() }
        sectionList.find { it.name == sectionName } ?: ConfigSection(sectionName).also { sectionList.add(it) }

        val sectionKey = "${categoryName}-${sectionName}"
        val subcategoryList = subcategories.getOrPut(sectionKey) { mutableListOf() }
        val subcategory = subcategoryList.find { it.name == subcategoryName } ?: ConfigSubcategory(subcategoryName).also { subcategoryList.add(it) }

        subcategory.elements.add(element)

        if (isSectionToggle && element.type is ElementType.Switch) {
            sectionToggleElements[sectionKey] = element.configKey
        }

        getDefaultValue(element.type)?.let { defaultValue ->
            if (!config.containsKey(element.configKey)) {
                config[element.configKey] = defaultValue
                dataUtils.setData(config)
                configListeners[element.configKey]?.forEach { it(defaultValue) }
            }
        }

        registerValidator(element)

        if (isFirstCategory) {
            activeCategory = categoryName
            performSearch()
            filteredSections[categoryName]?.firstOrNull()?.let {
                activeSection = it.name
                updateSections()
                updateElements()
            }
        } else {
            performSearch()
            if (activeCategory == categoryName) {
                updateSections()
                if (activeSection == sectionName) updateElements()
            }
        }
        return this
    }

    private fun registerValidator(element: ConfigElement) {
        val configValue = when (val type = element.type) {
            is ElementType.Switch -> ConfigValue.BooleanValue(type.default)
            is ElementType.Slider -> ConfigValue.DoubleValue(type.default, type.min, type.max)
            is ElementType.Dropdown -> ConfigValue.IntValue(type.default, 0, type.options.size - 1)
            is ElementType.TextInput -> ConfigValue.StringValue(type.default, type.maxLength)
            is ElementType.ColorPicker -> ConfigValue.ColorValue(type.default)
            is ElementType.Keybind -> ConfigValue.IntValue(type.default)
            else -> null
        }
        configValue?.let { validator.register(element.configKey, it) }
    }

    fun registerListener(configKey: String, listener: (Any) -> Unit): ConfigUI {
        configListeners.getOrPut(configKey) { mutableListOf() }.add(listener)
        (getConfigValue(configKey) ?: getDefaultValue(elementRefs[configKey]?.type))?.let { currentValue ->
            val resolvedValue = when (currentValue) {
                is Map<*, *> -> currentValue.toColorFromMap()
                else -> currentValue
            }
            resolvedValue?.let { listener(it) }
        }
        return this
    }

    fun getConfigValue(configKey: String): Any? = config[configKey]
    fun saveConfig() = dataUtils.save()

    private fun Color.withAlpha(alpha: Int): Color = Color(red, green, blue, alpha)
}