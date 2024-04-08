package com.runerealms.auth.command

import com.runerealms.auth.RuneAuth
import com.runerealms.auth.dao.Account
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
import org.jetbrains.exposed.sql.transactions.transaction

val RuneAuth.login get() = command("login", "logar") {
    val password by requiredArgument("password", ArgumentType.Text.Word)
    executor {
        val source = this.sender as Player

        if (authenticationStates[source.uniqueId] is AuthState.Authenticated) {
            source.sendMessage("§c§lERRO §fVocê já está logado.")
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
                val state = (authenticationStates[source.uniqueId] as AuthState.Unauthenticated)
                state.attempts++
                if (state.attempts >= 3) {
                    source.kick(
                        (Component.text("§c§lLIMITE EXCEDIDO")
                            .color(NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD))
                            .appendNewline()
                            .appendNewline()
                            .append(Component.text("Você excedeu o limite de tentativas de login."))
                    )
                    return@transaction
                }

                source.playSound(source.location, Sound.BLOCK_ANVIL_LAND, 1f, 1f)
                source.sendMessage("§c§lERRO §fSenha incorreta. Você tem mais ${3 - state.attempts} tentativas.")
                return@transaction
            }

            authenticationStates[source.uniqueId] = AuthState.Authenticated(premium = false)
            val event = PlayerAuthEvent(
                source,
                existingAccount
            )

            Bukkit.getPluginManager().callEvent(event)

            source.sendMessage("§a§lSUCESSO §fVocê se autenticou com sucesso!")
            source.clearTitle()
            source.showTitle(
                Title.title(
                    Component.text("LOGIN").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                    Component.text("Seja bem-vindo novamente!"),
                )
            )
        }
    }
}