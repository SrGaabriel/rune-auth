package com.runerealms.auth.command

import com.mojang.brigadier.arguments.StringArgumentType.word
import com.runerealms.auth.RuneAuth
import com.runerealms.auth.dao.Account
import com.runerealms.auth.event.PlayerAuthEvent
import com.runerealms.auth.struct.AuthState
import com.runerealms.core.feature.command.struct.types.ArgumentType
import com.runerealms.core.feature.command.util.command
import kotlinx.datetime.Clock
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.transaction

val RuneAuth.register get() = command("register", "registrar", "registro") {
    val password by requiredArgument("password", ArgumentType.Text.Word)
    executor {
        val source = this.sender as Player

        if (authenticationStates[source.uniqueId] is AuthState.Authenticated) {
            source.sendMessage("§c§lERRO §fVocê já está logado.")
            return@executor
        }

        if (password.length < 6) {
            source.playSound(source.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            source.sendMessage("§c§lERRO §fA senha precisa ter no mínimo §c6§f caracteres.")
            return@executor
        } else if (password.length > 16) {
            source.playSound(source.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            source.sendMessage("§c§lERRO §fA senha precisa ter no máximo §c16§f caracteres.")
            return@executor
        }

        transaction {
            val existingAccount = Account.findById(source.uniqueId)
            if (existingAccount != null) {
                source.playSound(source.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                source.sendMessage("§c§lERRO §fVocê já tem uma conta registrada.")
                return@transaction
            }
            val hashedPassword = hashingService.hashPassword(password)

            if (captchaManager != null) {
                (authenticationStates[source.uniqueId] as AuthState.Unauthenticated).captcha = true
                captchaManager!!.createCaptcha(source, hashedPassword)
            } else {
                val instant = Clock.System.now()
                val newAccount = Account.new(source.uniqueId) {
                    this.latestUsername = source.name
                    this.password = hashedPassword
                    this.createdAt = instant
                    this.updatedAt = instant
                }

                authenticationStates[source.uniqueId] = AuthState.Authenticated(premium = false)
                val event = PlayerAuthEvent(
                    source,
                    newAccount
                )

                Bukkit.getPluginManager().callEvent(event)

                source.sendMessage("§a§lSUCESSO §fConta registrada com sucesso.")
                source.clearTitle()
                source.showTitle(
                    Title.title(
                        Component.text("REGISTRADO").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                        Component.text("Você se registrou ao servidor com sucesso!"),
                    )
                )
            }
        }
    }
}