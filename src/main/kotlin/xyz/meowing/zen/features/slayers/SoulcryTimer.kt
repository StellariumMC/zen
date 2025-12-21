package xyz.meowing.zen.features.slayers

import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.zen.annotations.Module
import xyz.meowing.zen.api.item.ItemAbility
import xyz.meowing.zen.events.core.GuiEvent
import xyz.meowing.zen.events.core.SkyblockEvent
import xyz.meowing.zen.features.Feature
import xyz.meowing.zen.hud.HUDManager
import xyz.meowing.zen.utils.Render2D

@Module
object SoulcryTimer : Feature(
    "soulcryTimer",
    "Soulcry cooldown",
    "Shows Soulcry ability cooldown time",
    "Slayers",
    true
) {
    private const val NAME = "Soulcry Timer"

    override fun initialize() {
        HUDManager.register(NAME, "§cSoulcry: §c4.0s", "soulcryTimer")

        register<SkyblockEvent.ItemAbilityUsed> { event ->
            if (event.ability.abilityName.contains("Soulcry", ignoreCase = true)) {
                registerEvent("render")
            }
        }

        createCustomEvent<GuiEvent.Render.HUD.Pre>("render") {
            render(it.context)
        }
    }

    private fun render(context: GuiGraphics) {
        val text = getDisplayText()
        if (text.isEmpty()) return

        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)

        Render2D.renderStringWithShadow(context, text, x, y, scale)
    }

    private fun getDisplayText(): String {
        val timeLeftInSeconds = ItemAbility.getRemaining("Soulcry")

        if (timeLeftInSeconds > 0.0) {
            return "§cSoulcry: §c${"%.1f".format(timeLeftInSeconds)}s"
        }
        unregisterEvent("render")

        return ""
    }
}