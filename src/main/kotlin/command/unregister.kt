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

val RuneAuth.unregister get() = command("unregister", "desregistrar") {
    val password by requiredArgument("password", ArgumentType.Text.Word)
    executor {
        val source = this.sender as Player

        if (authenticationStates[source.uniqueId] is AuthState.Unauthenticated) {
            source.sendMessage("§c§lERRO §fVocê não está logado.")
            return@executor
        }

        transaction {
            val existingAccount = Account.findById(source.uniqueId)
            if (existingAccount == null) {
                source.playSound(source.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                source.sendMessage("§c§lERRO §fVocê não tem uma conta registrada. Use §c/registrar <senha>§f para criar uma.")
                return@transaction
            }

            if (existingAccount.password != hashingService.hashPassword(password)) {
                source.playSound(source.location, Sound.BLOCK_ANVIL_LAND, 1f, 1f)
                source.sendMessage("§c§lERRO §fSenha incorreta.")
                return@transaction
            }

            existingAccount.delete()
            source.kick(
                (Component.text("§c§lCANCELAMENTO")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD))
                    .appendNewline()
                    .appendNewline()
                    .append(Component.text("Sua conta foi cancelada."))
            )
        }
    }
}