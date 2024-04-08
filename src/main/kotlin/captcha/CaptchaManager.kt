package com.runerealms.auth.captcha

import org.bukkit.entity.Player

interface CaptchaManager {
    fun createCaptcha(player: Player, password: String)
}

