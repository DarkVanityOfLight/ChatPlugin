package com.github.darkvanityoflight.chatplugin.listeners


import com.github.darkvanityoflight.chatplugin.Main
import com.github.darkvanityoflight.chatplugin.chats.PlayerChat
import com.massivecraft.factions.FPlayers
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener(private val main: Main) : Listener {


    @EventHandler(priority = EventPriority.LOWEST)
    fun onMessage(event: AsyncPlayerChatEvent) {
        main.dataParser.update()
        val channel = main.dataParser.playerChannelMap[event.player.name]
        val chat: PlayerChat? = main.chats[channel]


        if (chat == null) {
            Bukkit.getLogger().warning("No channel with the name $channel could be found," +
                    " but was requested by player ${event.player.name}")
            event.isCancelled = true
            return
        }

        if (main.factionsEnabled) {
            val chatMode = FPlayers.getInstance().getByPlayer(event.player).chatMode

            if (chat.ignoreWorld) {
                event.format = chat.assembleMessage(event.message, event.player)
                event.recipients.removeAll(chat.getMutetPlayers())
                return
            } else {
                event.isCancelled = true
                chat.sendMessage(event.message, event.player)
                return
            }
        }else{
           chat.sendMessage(event.message, event.player)
            event.isCancelled = true
        }
    }
}
