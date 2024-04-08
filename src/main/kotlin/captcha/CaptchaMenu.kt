package com.runerealms.auth.captcha

import com.runerealms.auth.RuneAuth
import com.runerealms.core.feature.menu.menu
import com.runerealms.core.util.itemStack
import com.runerealms.core.util.name
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
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
                        player.kickPlayer(locale["kick.captcha-fail"])
                    }
                }
            }
        }
    }
    onClose {
        val completed = view.data["completed"] as? Boolean ?: error("Invalid completed state")
        if (!completed)
            reopen(false)

        captchaManager.onSuccessfulCaptcha(player, view.data["hashed-password"] as? String ?: error("Invalid hashed password"))
    }
}