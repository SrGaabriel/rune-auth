package com.runerealms.auth.command

import com.runerealms.auth.RuneAuth
import com.runerealms.auth.dao.Account
import com.runerealms.auth.minecraft.sessionId
import com.runerealms.auth.struct.AuthState
import com.runerealms.core.feature.command.util.command
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.transaction

val RuneAuth.premium get() = command("original", "premium") {
    executor {
        val source = this.sender as Player
        val authState = authenticationStates[source.uniqueId]

        if (authState !is AuthState.Authenticated) {
            source.sendMessage("§c§lERRO §fVocê não está logado.")
            return@executor
        }

        transaction {
            val account = Account.findById(source.uniqueId)
            if (account == null) {
                source.sendMessage("§c§lERRO §fVocê não tem uma conta registrada.")
                return@transaction
            }

            if (account.premium) {
                account.premium = false
                source.sendMessage("§c§lDESATIVADO §fVocê §cdesativou §fo modo de autenticação original.")
                return@transaction
            }

            val session = loginSessions[source.sessionId]
            if (session?.verified != true) {
                source.sendMessage("§c§lERRO §fVocê não está logado com uma conta original.")
                return@transaction
            }

            account.premium = true
            source.sendMessage("§6§lATIVADO §fVocê §6ativou §fo modo de autenticação original.")
        }
    }
}