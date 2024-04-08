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

        val wasAccountPremium = account.premium
        accountRepository.edit(account) {
            account.premium = !wasAccountPremium
        }
        if (wasAccountPremium) {
            source.sendMessage(locale["commands.premium.disabled"])
            return@executor
        }
        val session = loginSessions[source.sessionId]
        if (session?.verified != true) {
            source.sendMessage(locale["commands.premium.non-premium"])
            return@executor
        }
        source.sendMessage(locale["commands.premium.enabled"])
    }
}