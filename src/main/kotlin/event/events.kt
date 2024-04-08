package com.runerealms.auth.event

import com.runerealms.auth.dao.Account
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class PlayerAuthEvent(
    player: Player,
    val account: Account?
): PlayerEvent(player) {


    override fun getHandlers(): HandlerList = handlerList

    companion object {
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}

