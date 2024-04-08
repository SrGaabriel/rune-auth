package com.runerealms.auth.util

import com.comphenix.protocol.PacketType.Play
import com.runerealms.auth.RuneAuth
import com.runerealms.auth.struct.AuthState
import org.bukkit.entity.Player

fun Player.authState(plugin: RuneAuth): AuthState? =
    plugin.authenticationStates[uniqueId]