package com.runerealms.auth.captcha

import org.bukkit.entity.Player

interface CaptchaManager {
    fun createCaptcha(player: Player, hashedPassword: String)

    fun onSuccessfulCaptcha(player: Player, hashedPassword: String)
}

