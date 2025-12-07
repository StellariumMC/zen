package xyz.meowing.zen.api.item

import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.utils.extentions.stripColor
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.KnitClient.world
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.zen.annotations.Module
import xyz.meowing.zen.api.dungeons.DungeonAPI
import xyz.meowing.zen.api.dungeons.DungeonClass
import xyz.meowing.zen.api.location.SkyBlockIsland
import xyz.meowing.zen.api.skyblock.PlayerStats
import xyz.meowing.zen.events.*
import xyz.meowing.zen.events.core.ChatEvent
import xyz.meowing.zen.events.core.LocationEvent
import xyz.meowing.zen.events.core.MouseEvent
import xyz.meowing.zen.events.core.SkyblockEvent
import xyz.meowing.zen.utils.ItemUtils.lore
import xyz.meowing.zen.utils.SimpleTimeMark
import xyz.meowing.zen.utils.TickUtils
import xyz.meowing.zen.utils.TimeUtils
import xyz.meowing.zen.utils.TimeUtils.millis
import java.util.concurrent.ConcurrentHashMap

@Module
object ItemAbility {
    private val cooldowns = hashMapOf<String, CooldownItem>()
    private val activeCooldowns = ConcurrentHashMap<String, Double>()
    private var lastCooldownValues = ConcurrentHashMap<String, Double>()
    private var lastUpdateTimestamp: Long = 0L
    private var justUsedAbility: ItemAbility? = null
    private var cooldownReduction = -1
    private var lastAbilityType: String? = null

    data class CooldownItem(
        var sneakRightClick: ItemAbility? = null,
        var sneakLeftClick: ItemAbility? = null,
        var rightClick: ItemAbility? = null,
        var leftClick: ItemAbility? = null
    )

    data class ItemAbility(
        val itemId: String,
        var cooldownSeconds: Double = 0.0,
        var currentCount: Double = 0.0,
        var manaCost: Int = 0,
        var usedAt: SimpleTimeMark = TimeUtils.now,
        var abilityName: String = "Unknown",
        var type: String? = null
    )

    init {
        TickUtils.loop(10) {
            val player = player ?: return@loop
            world ?: return@loop

            lastCooldownValues.clear()
            lastCooldownValues.putAll(activeCooldowns)
            lastUpdateTimestamp = System.currentTimeMillis()

            activeCooldowns.replaceAll { _, cooldown -> updateCooldown(cooldown) }
            activeCooldowns.entries.removeIf { it.value <= 0 }

            repeat(9) { i ->
                val stack = player.inventory.getItem(i)
                if (!stack.isEmpty) setStackCooldown(stack)
            }
        }

        EventBus.register<LocationEvent.WorldChange> {
            activeCooldowns.clear()
            cooldowns.clear()
            cooldownReduction = -1
        }

        EventBus.register<MouseEvent.Click> { event ->
            if (world == null) return@register
            if (client.screen != null) return@register

            val cdItem = cooldowns[player?.mainHandItem?.getData(DataTypes.SKYBLOCK_ID)?.skyblockId] ?: return@register
            val sneaking = player?.isShiftKeyDown == true

            val ability = when (event.button) {
                0 -> if (sneaking) cdItem.sneakLeftClick else cdItem.leftClick
                1 -> if (sneaking) cdItem.sneakRightClick else cdItem.rightClick
                else -> null
            } ?: return@register

            val remainingCooldown = activeCooldowns[ability.abilityName]
            if (remainingCooldown != null && remainingCooldown > 0.0) {
                return@register
            }

            if (ability.manaCost > PlayerStats.mana) return@register

            EventBus.post(SkyblockEvent.ItemAbilityUsed(ability))
            justUsedAbility = ability
            activeCooldowns[ability.abilityName] = ability.cooldownSeconds
            lastCooldownValues[ability.abilityName] = ability.cooldownSeconds
            lastUpdateTimestamp = System.currentTimeMillis()
        }

        EventBus.register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register
            val clean = event.message.stripped

            if (clean.startsWith("Used") && SkyBlockIsland.THE_CATACOMBS.inIsland()) {
                justUsedAbility = ItemAbility("Dungeon_Ability")
            }

            justUsedAbility?.let { ability ->
                val skyblockId = player?.mainHandItem?.getData(DataTypes.SKYBLOCK_ID)?.skyblockId ?: return@register

                if (
                    ability.itemId == skyblockId &&
                    clean.startsWith("This ability is on cooldown for") &&
                    ability.usedAt.since.millis <= 300
                ) {
                    val currentCooldown = clean.replace("[^0-9]".toRegex(), "").toInt()
                    ability.currentCount = ability.cooldownSeconds - currentCooldown
                    activeCooldowns[ability.abilityName] = currentCooldown.toDouble()
                }
            }
        }
    }

    private fun setItemAbility(line: String, cdItem: CooldownItem, skyblockId: String) {
        val remaining = line.split("Ability: ").getOrNull(1) ?: return
        val abilityEnd = remaining.lastIndexOf("  ").takeIf { it != -1 } ?: return

        val abilityName = remaining.take(abilityEnd).trim()
        val type = remaining.drop(abilityEnd).trim()

        lastAbilityType = type
        val ability = ItemAbility(skyblockId, abilityName = abilityName, type = type)

        when (type) {
            "RIGHT CLICK" -> cdItem.rightClick = ability
            "LEFT CLICK" -> cdItem.leftClick = ability
            "SNEAK RIGHT CLICK" -> cdItem.sneakRightClick = ability
            "SNEAK LEFT CLICK" -> cdItem.sneakLeftClick = ability
        }
    }

    private fun setCooldownSeconds(clean: String, cdItem: CooldownItem) {
        val cooldownSeconds = clean.replace("[^0-9]".toRegex(), "").toInt().toDouble()
        abilityByType(cdItem, lastAbilityType)?.cooldownSeconds = cooldownSeconds
    }

    private fun setManaCost(clean: String, cdItem: CooldownItem) {
        val manaCost = clean.replace("[^0-9]".toRegex(), "").toInt()
        abilityByType(cdItem, lastAbilityType)?.manaCost = manaCost
    }

    private fun abilityByType(cdItem: CooldownItem, type: String?): ItemAbility? = when (type) {
        "RIGHT CLICK" -> cdItem.rightClick
        "LEFT CLICK" -> cdItem.leftClick
        "SNEAK RIGHT CLICK" -> cdItem.sneakRightClick
        "SNEAK LEFT CLICK" -> cdItem.sneakLeftClick
        else -> null
    }

    private fun setStackCooldown(item: ItemStack) {
        if (world == null) return
        val skyblockId = item.getData(DataTypes.SKYBLOCK_ID)?.skyblockId.takeIf { !cooldowns.containsKey(it) } ?: return
        val cdItem = CooldownItem()
        var hasAbility = false

        item.lore.forEach { line ->
            val clean = line.stripColor()

            when {
                clean.contains("Ability: ") -> {
                    setItemAbility(clean, cdItem, skyblockId)
                    hasAbility = true
                }

                clean.contains("Cooldown: ") && hasAbility -> {
                    setCooldownSeconds(clean, cdItem)
                }

                clean.contains("Mana Cost: ") && hasAbility -> {
                    setManaCost(clean, cdItem)
                }
            }
        }

        if (
            hasAbility &&
            listOf(cdItem.rightClick, cdItem.leftClick, cdItem.sneakRightClick, cdItem.sneakLeftClick).any { it != null }
        ) {
            cooldowns[skyblockId] = cdItem
        }
    }

    private fun updateCooldown(cooldownCount: Double): Double {
        var secondsToAdd = 0.5

        if (
            SkyBlockIsland.THE_CATACOMBS.inIsland() &&
            cooldownReduction == -1 &&
            DungeonAPI.dungeonClass == DungeonClass.MAGE
        ) {
            cooldownReduction = (DungeonAPI.classLevel / 2) + 25
            if (DungeonAPI.uniqueClass) cooldownReduction += 25
        }

        if (cooldownReduction != -1) secondsToAdd *= (100.0 + cooldownReduction) / cooldownReduction

        return cooldownCount - secondsToAdd
    }

    fun getRemaining(abilityName: String): Double {
        val lastAccurateTime = lastCooldownValues.entries
            .firstOrNull { it.key.contains(abilityName, ignoreCase = true) }
            ?.value ?: 0.0
        if (lastAccurateTime <= 0.0) return 0.0

        val timeSinceLastUpdate = (System.currentTimeMillis() - lastUpdateTimestamp) / 1000.0
        val interpolatedTime = lastAccurateTime - timeSinceLastUpdate

        if (interpolatedTime < 0.0) {
            return activeCooldowns[abilityName]?.takeIf { it > 0.0 } ?: 0.0
        }

        return interpolatedTime
    }
}