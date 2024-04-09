package com.runerealms.auth.captcha

import com.runerealms.auth.RuneAuth
import com.runerealms.auth.dao.Account
import com.runerealms.auth.event.PlayerAuthEvent
import com.runerealms.auth.minecraft.sessionId
import com.runerealms.auth.struct.AuthState
import kotlinx.datetime.Clock
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.transaction

open class CategoryCaptchaManager(private val plugin: RuneAuth): CaptchaManager {
    public var menu = plugin.categoryCaptcha

    override fun createCaptcha(player: Player, hashedPassword: String) {
        menu.open(player, data = mutableMapOf(
            "completed" to false,
            "categories" to DefaultCaptchaCategories.All,
            "hashed-password" to hashedPassword
        ))
    }

    override fun onSuccessfulCaptcha(player: Player, hashedPassword: String) {
        val instant = Clock.System.now()
        val newAccount = plugin.accountRepository.insert(player.uniqueId) { account ->
            account.latestUsername = player.name
            account.password = hashedPassword
            account.createdAt = instant
            account.updatedAt = instant
        }

        plugin.authenticationStates[player.uniqueId] = AuthState.Authenticated(premium = false)
        val event = PlayerAuthEvent(
            player,
            newAccount
        )
        Bukkit.getPluginManager().callEvent(event)

        player.sendMessage(plugin.locale["commands.successful-register"])

        val hasPremiumAccount = plugin.loginSessions[player.sessionId]?.verified == true
        if (hasPremiumAccount) {
            player.sendActionBar(Component.text(plugin.locale["commands.premium-tip"]))
        }

        player.clearTitle()
        player.showTitle(
            Title.title(
                Component.text(plugin.locale["titles.register.title"]),
                Component.text(plugin.locale["titles.register.subtitle"])
            )
        )
        return
    }
}