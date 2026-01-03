package xyz.meowing.zen.features.mining

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.sounds.SoundEvents
import xyz.meowing.zen.annotations.Module
import xyz.meowing.zen.api.item.ItemAbility
import xyz.meowing.zen.events.core.GuiEvent
import xyz.meowing.zen.events.core.TablistEvent
import xyz.meowing.zen.features.Feature
import xyz.meowing.zen.hud.HUDManager
import xyz.meowing.zen.utils.Render2D
import xyz.meowing.zen.utils.ScoreboardUtils
import xyz.meowing.zen.utils.TitleUtils.showTitle
import xyz.meowing.zen.utils.Utils

@Module
object MiningAbility : Feature(
    "miningAbility",
    "Mining ability",
    "Mining ability cooldown tracker",
    "Mining",
    skyblockOnly = true
) {
    private const val NAME = "Mining Ability"
    private val showTitle by config.switch("Show title")
    private val miningAbilities = setOf(
        "Mining Speed Boost",
        "Pickobulus",
        "Tunnel Vision",
        "Maniac Miner",
        "Gemstone Infusion",
        "Sheer Force"
    )

    private var hasWidget: Boolean = false
    private var wasOnCooldown: Boolean = false
    private var abilityName: String = ""

    override fun initialize() {
        HUDManager.register(NAME, "§9§lPickaxe Ability:\n§fMining Speed Boost: §aAvailable", "miningAbility")

        register<TablistEvent.Change> { parseTablist() }

        register<GuiEvent.Render.HUD.Pre> { event ->
            if (hasWidget) {
                render(event.context)
            }
        }
    }

    private fun render(context: GuiGraphics) {
        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)

        getDisplayLines().forEachIndexed { index, line ->
            Render2D.renderStringWithShadow(context, line, x, y + index * 10 * scale, scale)
        }
    }


    private fun parseTablist() {
        val entries = ScoreboardUtils.getTabListEntriesString()

        val abilityIndex = entries.indexOfFirst { it.contains("Ability", ignoreCase = true) }

        if (abilityIndex == -1 || abilityIndex + 1 >= entries.size) {
            hasWidget = false
            reset()
            return
        }

        val abilityLine = entries[abilityIndex + 1]

        if (!abilityLine.contains(":")) return

        val parts = abilityLine.split(":", limit = 2)
        if (parts.size != 2) return

        val parsedAbilityName = parts[0].trim()

        if (miningAbilities.contains(parsedAbilityName)) {
            hasWidget = true
            abilityName = parsedAbilityName
        } else {
            hasWidget = false
            reset()
        }
    }

    private fun getDisplayLines(): List<String> {
        if (!hasWidget || abilityName.isEmpty()) {
            return listOf(
                "§9§lPickaxe Ability:",
                "§cNONE"
            )
        }

        val remaining = ItemAbility.getRemaining(abilityName)
        val isAvailable = remaining <= 0.0

        if (isAvailable && wasOnCooldown && showTitle) {
            showTitle("§aAbility Ready!", null, 2000)
            Utils.playSound(SoundEvents.CAT_AMBIENT, 1f, 1f)
            wasOnCooldown = false
        } else if (!isAvailable) {
            wasOnCooldown = true
        }

        val statusText = if (isAvailable) {
            "§a§lAvailable"
        } else {
            val color = when {
                remaining <= 3.0 -> "§c"
                remaining <= 10.0 -> "§e"
                else -> "§6"
            }
            val timeText = if (remaining <= 5.0) "%.1fs".format(remaining) else "${remaining.toInt()}s"
            "$color$timeText"
        }

        return listOf(
            "§9§lPickaxe Ability:",
            "§f $abilityName: $statusText"
        )
    }

    private fun reset() {
        abilityName = ""
        wasOnCooldown = false
    }
}