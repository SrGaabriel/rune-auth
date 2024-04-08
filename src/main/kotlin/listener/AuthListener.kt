package com.runerealms.auth.listener

import com.runerealms.auth.RuneAuth
import com.runerealms.auth.dao.Account
import com.runerealms.auth.event.PlayerAuthEvent
import com.runerealms.auth.minecraft.sessionId
import com.runerealms.auth.struct.AuthState
import com.runerealms.auth.util.authState
import com.runerealms.core.ext.inWholeTicks
import com.runerealms.core.ext.inWholeTicksInt
import com.runerealms.core.ext.ticks
import com.runerealms.core.feature.command.Commands
import com.runerealms.core.feature.menu.Menus
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.Times
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.potion.PotionEffect
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class AuthListener(private val plugin: RuneAuth): Listener {
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.loginSessions.remove(event.player.sessionId)
        plugin.authenticationStates.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (event.player.authState(plugin) is AuthState.Unauthenticated) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerChat(event: AsyncChatEvent) {
        if (event.player.authState(plugin) is AuthState.Unauthenticated) {
            event.isCancelled = true
            event.player.sendMessage("§c§lERRO §fVocê precisa estar logado para falar no chat.")
        }
    }

    @EventHandler
    fun onPlayerCommand(event: PlayerCommandPreprocessEvent) {
        if (event.player.authState(plugin) is AuthState.Unauthenticated) {
            val authCommand = plugin.feature(Commands).repository.search(event.message.substring(1).split(" ")[0])
            if (authCommand != null) {
                return
            }

            event.isCancelled = true
            event.player.sendMessage("§c§lERRO §fVocê precisa estar logado para executar comandos.")
        }
    }

    @EventHandler
    fun onPlayerAuth(event: PlayerAuthEvent) {
        event.player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS)
        event.player.playSound(event.player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        transaction {
            val account = Account.findById(event.player.uniqueId)

            val premiumSession = plugin.loginSessions[event.player.sessionId]
            if (account != null && account.premium && premiumSession?.verified == true) {
                val authEvent = PlayerAuthEvent(event.player, null)
                Bukkit.getPluginManager().callEvent(authEvent)

                plugin.authenticationStates[event.player.uniqueId] = AuthState.Authenticated(true)
                event.player.showTitle(
                    Title.title(
                        Component.text("§6§lORIGINAL").decorate(TextDecoration.BOLD),
                        Component.text("Sua conta é §6original§f, você não precisa se autenticar")
                    )
                )
                event.player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS)
                event.player.playSound(event.player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                return@transaction
            }

            plugin.authenticationStates[event.player.uniqueId] = AuthState.Unauthenticated(attempts = 0, captcha = false)

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, {
                if (event.player.authState(plugin) is AuthState.Unauthenticated) {
                    event.player.kick(
                        Component.text("§c§lERRO")
                            .appendNewline()
                            .appendNewline()
                            .append(Component.text("Você demorou muito tempo para logar"))
                    )
                }
            }, 30.seconds.inWholeTicks)

            event.player.addPotionEffect(PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 1.hours.inWholeTicksInt, 1))

            if (account == null) {
                event.player.showTitle(Title.title(
                    Component.text("REGISTRO").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                    Component
                        .text("Faça seu cadastro utilizando ")
                        .color(NamedTextColor.WHITE)
                        .append(
                            Component.text("/registrar <senha>").color(NamedTextColor.YELLOW)
                        )
                        .append(Component.text(" para começar a jogar.")),
                        Times.times(5.ticks.toJavaDuration(), 30.seconds.toJavaDuration(), 10.ticks.toJavaDuration())
                ))
                return@transaction
            }

            event.player.showTitle(Title.title(
                Component.text("LOGIN").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component
                    .text("Faça seu login utilizando ")
                    .color(NamedTextColor.WHITE)
                    .append(
                        Component.text("/login <senha>").color(NamedTextColor.YELLOW)
                    )
                    .append(Component.text(" para começar a jogar.")),
                    Times.times(5.ticks.toJavaDuration(), 30.seconds.toJavaDuration(), 10.ticks.toJavaDuration())
            ))

            if (account.latestUsername != event.player.name) {
                val oldName = account.latestUsername
                account.latestUsername = event.player.name
                event.player.sendMessage("§3§lMIGRAÇÃO §7Seus dados da sua conta anterior §3${oldName} §fforam migrados com sucesso!")
            }
        }
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player as? Player ?: return
        val state = player.authState(plugin)
        if (state !is AuthState.Unauthenticated) {
            return
        }
        event.isCancelled = !state.captcha
    }
}