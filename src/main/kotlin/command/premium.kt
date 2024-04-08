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
            source.sendMessage(locale["commands.not-logged"])
            return@executor
        }

        val account = accountRepository[source.uniqueId]
        if (account == null) {
            source.sendMessage(locale["commands.no-account"])
            return@executor
        }

        accountRepository.edit(account) {
            if (account.premium) {
                account.premium = false
                source.sendMessage(locale["commands.premium.disabled"])
                return@edit
            }

            val session = loginSessions[source.sessionId]
            if (session?.verified != true) {
                source.sendMessage(locale["commands.premium.non-premium"])
                return@edit
            }
            account.premium = true
        }
        source.sendMessage(locale["commands.premium.enabled"])
    }
}