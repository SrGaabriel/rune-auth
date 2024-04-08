package com.runerealms.auth.listener

import com.comphenix.protocol.wrappers.WrappedGameProfile
import com.comphenix.protocol.wrappers.WrappedSignedProperty
import com.runerealms.auth.RuneAuth
import com.runerealms.auth.event.PlayerAuthEvent
import com.runerealms.auth.minecraft.SkinProperty
import com.runerealms.core.RunePlugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class SkinListener(private val plugin: RuneAuth): Listener {
    @EventHandler(priority = EventPriority.LOW)
    fun onJoin(event: PlayerAuthEvent) {
        for (session in plugin.loginSessions.values) {
            if (session.verifiedUsername == event.player.name) {
                val skinProperty = session.skinProperty
                if (skinProperty != null) {
                    updateSkin(event.player, skinProperty.value, skinProperty.signature)
                }
            }
        }
    }

    private fun updateSkin(player: Player, data: String, signature: String) {
        val profile = WrappedGameProfile.fromPlayer(player)
        val skin = WrappedSignedProperty.fromValues("textures", data, signature)
        profile.properties.put("textures", skin)
    }
}