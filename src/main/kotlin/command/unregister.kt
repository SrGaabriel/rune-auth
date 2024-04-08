package com.runerealms.auth.command

import com.runerealms.auth.RuneAuth
import com.runerealms.auth.struct.AuthState
import com.runerealms.core.feature.command.struct.types.ArgumentType
import com.runerealms.core.feature.command.util.command
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Sound
import org.bukkit.entity.Player

val RuneAuth.unregister get() = command("unregister", "desregistrar") {
    val password by requiredArgument("senha", ArgumentType.Text.Word)
    executor {
        val source = this.sender as Player

        if (authenticationStates[source.uniqueId] is AuthState.Unauthenticated) {
            source.sendMessage(locale["commands.not-logged"])
            return@executor
        }

        val existingAccount = accountRepository[source.uniqueId]
        if (existingAccount == null) {
            source.playSound(source.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            source.sendMessage(locale["commands.no-account"])
            return@executor
        }

        if (existingAccount.password != hashingService.hashPassword(password)) {
            source.playSound(source.location, Sound.BLOCK_ANVIL_LAND, 1f, 1f)
            source.sendMessage(locale["commands.incorrect-password"])
            return@executor
        }

        accountRepository.remove(source.uniqueId)
        source.kickPlayer(locale["kick.account-cancelled"])
    }
}