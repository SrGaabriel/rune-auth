package com.runerealms.auth.command

import com.runerealms.auth.RuneAuth
import com.runerealms.auth.event.PlayerAuthEvent
import com.runerealms.auth.struct.AuthState
import com.runerealms.core.feature.command.struct.types.ArgumentType
import com.runerealms.core.feature.command.util.command
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player

val RuneAuth.login get() = command("login", "logar") {
    val password by requiredArgument("senha", ArgumentType.Text.Word)
    executor {
        val source = this.sender as Player

        if (authenticationStates[source.uniqueId] is AuthState.Authenticated) {
            source.sendMessage(locale["commands.already-logged"])
            return@executor
        }

        val existingAccount = accountRepository[source.uniqueId]
        if (existingAccount == null) {
            source.playSound(source.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            source.sendMessage(locale["commands.no-account"])
            return@executor
        }

        if (existingAccount.password != hashingService.hashPassword(password)) {
            val state = (authenticationStates[source.uniqueId] as AuthState.Unauthenticated)
            state.attempts++
            if (state.attempts >= 3) {
                source.kickPlayer(locale["kick.excessive-attempts"])
                return@executor
            }

            source.playSound(source.location, Sound.BLOCK_ANVIL_LAND, 1f, 1f)
            source.sendMessage(locale["commands.incorrect-password"] + " " + locale.key("commands.attempts-left", (3 - state.attempts).toString()))
            return@executor
        }

        authenticationStates[source.uniqueId] = AuthState.Authenticated(premium = false)
        val event = PlayerAuthEvent(
            source,
            existingAccount
        )

        Bukkit.getPluginManager().callEvent(event)

        source.sendMessage(locale["commands.successful-login"])
        source.clearTitle()
        source.showTitle(
            Title.title(
                Component.text(locale["titles.login.title"]),
                Component.text(locale["titles.login.subtitle"])
            )
        )
    }
}