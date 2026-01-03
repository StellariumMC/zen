package xyz.meowing.zen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.fabricmc.api.ClientModInitializer
import xyz.meowing.zen.events.EventBus
import xyz.meowing.zen.features.Debug
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.loader.KnitModInfo
import xyz.meowing.knit.api.scheduler.TimeScheduler
import xyz.meowing.knit.api.text.KnitText
import xyz.meowing.zen.api.data.StoredFile
import xyz.meowing.zen.events.core.GameEvent
import xyz.meowing.zen.events.core.ServerEvent
import xyz.meowing.zen.managers.config.ConfigManager
import xyz.meowing.zen.managers.feature.FeatureManager
import xyz.meowing.zen.updateChecker.UpdateChecker

object Zen : ClientModInitializer {
    private var showLoad = true

    const val MODRINTH_PROJECT_ID = "stWFyj4m"

    const val GITHUB_REPO = "StellariumMC/zen"

    @JvmStatic
    val modInfo = KnitModInfo("zen", "Zen", "1.3.0")

    @JvmStatic
    val prefix = "§7[§bZen§7]"

    @JvmStatic
    val saveData = StoredFile("main/Main")

    @JvmStatic
    var isFirstInstall: Boolean by saveData.boolean("firstInstall", true)
        private set

    @JvmField
    val LOGGER: Logger = LogManager.getLogger("zen")

    @JvmStatic
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onInitializeClient() {
        EventBus.post(GameEvent.ModInit.Pre())

        ConfigManager.createConfigUI()
        FeatureManager.loadFeatures()
        FeatureManager.initializeFeatures()
        ConfigManager.executePending()

        EventBus.register<ServerEvent.Connect> {
            if (!showLoad) return@register

            val loadMessage = KnitText
                .literal("$prefix §fMod loaded.")
                .onHover("§c${FeatureManager.moduleCount} modules §8- §c${FeatureManager.loadTime}ms §8- §c${FeatureManager.commandCount} commands")
                .toVanilla()

            KnitChat.fakeMessage(loadMessage)

            if (Debug.debugMode) KnitChat.fakeMessage("$prefix §fYou have debug mode enabled, run §c/zendebug toggle§f to disable.")

            showFirstInstall()

            TimeScheduler.schedule(5000) {
                UpdateChecker.check()
            }

            showLoad = false
        }

        EventBus.post(GameEvent.ModInit.Post())
    }

    private fun showFirstInstall() {
        if (isFirstInstall) {
            KnitChat.fakeMessage("$prefix §fThanks for installing Zen!")
            KnitChat.fakeMessage("§7> §fUse §c/zen §fto open the config or §c/zen hud §fto edit HUD elements")

            val discordMessage = KnitText
                .literal("§7> §cDiscord:§b [Discord]")
                .onHover("Discord server")
                .onClick("https://discord.gg/KPmHQUC97G")

            KnitChat.fakeMessage(discordMessage)
            isFirstInstall = false
        }
    }
}