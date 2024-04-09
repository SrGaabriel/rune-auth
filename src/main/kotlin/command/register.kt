package com.runerealms.auth.command

import com.runerealms.auth.RuneAuth
import com.runerealms.auth.struct.AuthState
import com.runerealms.core.feature.command.struct.types.ArgumentType
import com.runerealms.core.feature.command.util.command
import org.bukkit.Sound
import org.bukkit.entity.Player

val RuneAuth.register get() = command("register", "registrar", "registro") {
    val password by requiredArgument("senha", ArgumentType.Text.Word)
    executor {
        val source = this.sender as Player
        val authState = authenticationStates[source.uniqueId]

        if (authState is AuthState.Authenticated) {
            source.sendMessage(locale["commands.already-logged"])
            return@executor
        }

        if (password.length < 6) {
            source.playSound(source.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            source.sendMessage(locale.key("commands.register.password-too-short", "6"))
            return@executor
        } else if (password.length > 16) {
            source.playSound(source.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            source.sendMessage(locale.key("commands.register.password-too-long", "16"))
            return@executor
        }

        val existingAccount = accountRepository[source.uniqueId]
        if (existingAccount != null) {
            source.playSound(source.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            source.sendMessage(locale["commands.already-registered"])
            return@executor
        }
        val hashedPassword = hashingService.hashPassword(password)

        (authState as? AuthState.Unauthenticated ?: return@executor).captcha = true
        captchaManager.createCaptcha(source, hashedPassword)
    }
}