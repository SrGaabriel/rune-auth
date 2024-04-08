package com.runerealms.auth.util

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.AdventureComponentConverter
import net.kyori.adventure.text.Component

fun PacketEvent.disconnectPlayer(reason: Component) {
    val kickPacket = PacketContainer(PacketType.Login.Server.DISCONNECT)
    kickPacket.chatComponents.write(0, AdventureComponentConverter.fromComponent(reason))
    ProtocolLibrary.getProtocolManager().sendServerPacket(player, kickPacket)
}