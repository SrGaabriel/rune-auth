package com.runerealms.auth.captcha

import com.runerealms.auth.RuneAuth
import com.runerealms.auth.captcha.CaptchaCategory
import com.runerealms.auth.dao.Account
import com.runerealms.auth.event.PlayerAuthEvent
import com.runerealms.auth.struct.AuthState
import com.runerealms.core.feature.menu.RuneMenuView
import com.runerealms.core.feature.menu.menu
import com.runerealms.core.util.itemStack
import com.runerealms.core.util.name
import kotlinx.datetime.Clock
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Material
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

@Suppress("unchecked_cast")
val RuneAuth.categoryCaptcha get() = menu(
    size = 9
) {
    onRender {
        val categories = (data["categories"] as? Collection<CaptchaCategory>)?: error("Invalid categories state")
        val category = categories.random()
        title = Component.text("CAPTCHA ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
            .append(Component.text("- Tema: ").color(NamedTextColor.GRAY))
            .append(Component.text(category.name.lowercase()).color(NamedTextColor.YELLOW))

        val chosenItem = category.materials.random()
        val itemSlot = Random.nextInt(0, size!!)
        val availableItems = (categories - category).flatMap { it.materials }.toMutableSet()

        for (i in 0 until size!!) {
            if (i == itemSlot) {
                item(i, itemStack(chosenItem).name("§kabcde")) {
                    onClick {
                        view.data["completed"] = true
                        view.close()
                    }
                }
            } else {
                val chosen = availableItems.random()
                availableItems.remove(chosen)
                item(i, itemStack(chosen).name("§kabcde")) {
                    onClick {
                        player.kick(
                            (Component.text("§c§lERRO")
                                .color(NamedTextColor.RED)
                                .decorate(TextDecoration.BOLD))
                                .appendNewline()
                                .appendNewline()
                                .append(Component.text("Você errou o CAPTCHA. Tente novamente mais tarde."))
                        )
                    }
                }
            }
        }
    }
    onClose {
        val completed = view.data["completed"]
        if (completed == true) {
            val instant = Clock.System.now()
            val newAccount = transaction {
                Account.new(player.uniqueId) {
                    this.latestUsername = player.name
                    println(view.data)
                    this.password = view.data["hashed-password"] as? String ?: error("Invalid hashed password state")
                    this.createdAt = instant
                    this.updatedAt = instant
                }
            }

            authenticationStates[player.uniqueId] = AuthState.Authenticated(premium = false)
            val event = PlayerAuthEvent(
                player,
                newAccount
            )

            Bukkit.getPluginManager().callEvent(event)

            player.sendMessage("§a§lSUCESSO §fConta registrada com sucesso.")
            player.clearTitle()
            player.showTitle(
                Title.title(
                    Component.text("REGISTRADO").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                    Component.text("Você se registrou ao servidor com sucesso!"),
                )
            )
            return@onClose
        }
        reopen(false)
    }
}