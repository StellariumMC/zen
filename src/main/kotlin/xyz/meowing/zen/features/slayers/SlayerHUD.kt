package xyz.meowing.zen.features.slayers

import xyz.meowing.zen.features.Feature
import xyz.meowing.zen.hud.HUDManager
import xyz.meowing.zen.utils.Render2D.width
import xyz.meowing.zen.utils.Utils.removeFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.Entity
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.KnitClient.world
import xyz.meowing.zen.annotations.Module
import xyz.meowing.zen.events.core.GuiEvent
import xyz.meowing.zen.events.core.SkyblockEvent

@Module
object SlayerHUD : Feature(
    "slayerHud",
    "Slayer HUD",
    "Displays information about the current slayer boss",
    "Slayers",
    skyblockOnly = true
) {
    private const val NAME = "Slayer HUD"
    private var timerEntity: Entity? = null
    private var hpEntity: Entity? = null
    private var bossID: Int? = null

    override fun initialize() {
        HUDManager.register(NAME, "§c02:59\n§c☠ §bVoidgloom Seraph IV §e64.2M§c❤", "slayerHud")

        createCustomEvent<GuiEvent.Render.HUD.Pre>("render") {
            render(it.context)
        }

        register<SkyblockEvent.Slayer.Spawn> { event ->
            val world = world ?: return@register
            bossID = event.entityID
            timerEntity = world.getEntity(event.entityID - 1)
            hpEntity = world.getEntity(event.entityID - 2)
            registerEvent("render")
        }

        register<SkyblockEvent.Slayer.Death> {
            unregisterEvent("render")
            bossID = null
        }

        register<SkyblockEvent.Slayer.Fail> {
            unregisterEvent("render")
            bossID = null
        }

        register<SkyblockEvent.Slayer.Cleanup> {
            unregisterEvent("render")
            bossID = null
        }
    }

    private fun render(context: GuiGraphics) {
        val timeText = timerEntity?.name ?: return
        val hpText = hpEntity?.name ?: return
        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)
        val matrices = context.pose()
        //#if MC >= 1.21.7
        //$$ matrices.pushMatrix()
        //$$ matrices.translate(x, y)
        //$$ matrices.scale(scale, scale)
        //#else
        matrices.pushPose()
        matrices.translate(x, y, 0f)
        matrices.scale(scale, scale, 1f)
        //#endif

        val hpWidth = hpText.string.removeFormatting().width()
        val timeWidth = timeText.string.removeFormatting().width()

        context.drawString(client.font, timeText, (hpWidth - timeWidth) / 2, 0, -1, false)
        context.drawString(client.font, hpText, 0, 10, -1, false)

        //#if MC >= 1.21.7
        //$$ matrices.popMatrix()
        //#else
        matrices.popPose()
        //#endif
    }
}
