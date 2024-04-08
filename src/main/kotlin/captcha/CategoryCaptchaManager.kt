package com.runerealms.auth.captcha

import com.runerealms.auth.RuneAuth
import org.bukkit.entity.Player

class CategoryCaptchaManager(plugin: RuneAuth): CaptchaManager {
    private val menu = plugin.categoryCaptcha

    override fun createCaptcha(player: Player, password: String) {
        menu.open(player, data = mutableMapOf(
            "categories" to DefaultCaptchaCategories.All,
            "hashed-password" to password
        ))
    }
}