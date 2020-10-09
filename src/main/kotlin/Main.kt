package com.github.DarkVanityOfLight.ChattPlugin

import com.github.DarkVanityOfLight.ChattPlugin.chats.PlayerChat
import com.github.DarkVanityOfLight.ChattPlugin.chats.SpyChat
import com.github.DarkVanityOfLight.ChattPlugin.commands.ListChannels
import com.github.DarkVanityOfLight.ChattPlugin.commands.MuteChannel
import com.github.DarkVanityOfLight.ChattPlugin.commands.SwitchChannel
import com.github.DarkVanityOfLight.ChattPlugin.commands.UnmuteChannel
import com.github.DarkVanityOfLight.ChattPlugin.listeners.ChatListener
import com.github.DarkVanityOfLight.ChattPlugin.listeners.ServerLoadListener
import com.github.DarkVanityOfLight.ChattPlugin.parser.ConfigParser
import com.github.DarkVanityOfLight.ChattPlugin.parser.DataParser
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.io.File
import java.lang.reflect.Field
import com.github.darkvanityoflight.darkmodcore.ADarkMod
import com.github.darkvanityoflight.darkmodcore.configparser.ADarkModConfigParser


class Main : ADarkMod(), Listener {
    override var configParser : ADarkModConfigParser = ConfigParser(this)
    val chats : MutableMap<String, PlayerChat> = emptyMap<String, PlayerChat>().toMutableMap()
    val dataParser : DataParser = DataParser(this)
    private val chatLog : File = File(this.dataFolder.absolutePath + "/log.lst")
    lateinit var spyChat: SpyChat
    var factionsEnabled: Boolean = false
    var luckPermsEnabled: Boolean = false
    var luckPermApi : LuckPerms? = null

    override fun onEnable(){
        super.onEnable()
        configParser.read()

        if (!chatLog.exists()){
            chatLog.createNewFile()
        }

        if (Bukkit.getPluginManager().isPluginEnabled("Factions")) factionsEnabled = true
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")){
            luckPermsEnabled = true

            val luckPermProvider = Bukkit.getServicesManager().getRegistration(
                LuckPerms::class.java
            )
            if (luckPermProvider != null) {
                luckPermApi = luckPermProvider.provider
            }else{
                warning("Could not get the Lucky perm api even though lucky perms is enabled")
            }
        }

        // Check if data file exists if not create
        val f = File(dataFolder.absolutePath + "/data.yml")
        if (!f.exists()){
            f.createNewFile()
        }
        dataParser.update()

        spyChat = SpyChat(this)

        val bukkitCommandMap: Field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
        bukkitCommandMap.isAccessible = true
        val commandMap: CommandMap = bukkitCommandMap.get(Bukkit.getServer()) as CommandMap

        // Create chat obj for every chat defined in the config file
        for (channel in configParser.chats) {
            val properties = configParser.chatProperties[channel]
            if (properties != null) {
                if ("ignore_world" in properties.keys){
                    chats[channel.toUpperCase()] = PlayerChat(
                        properties["name"] as String, properties["list_style"] as String,
                        properties["ignore_world"] as Boolean, properties["format"] as String,
                        properties["muteable"] as Boolean, properties["radius"] as Int, this, channel
                    )
                } else{
                    chats[channel.toUpperCase()] = PlayerChat(
                        properties["name"] as String, properties["list_style"] as String,
                        properties["format"] as String, properties["muteable"] as Boolean,
                        properties["radius"] as Int, this, channel
                    )
                }
                if (!configParser.overwrite!!){
                    // Register our commands without plugin.yml
                    commandMap.register(
                        channel, SwitchChannel(
                            channel, properties["list_style"] as String, "/$channel",
                            "chatPlugin.commands.switchChannel", ArrayList(), this
                        )
                    )
                }
            }else {
                warning("No properties ofr $channel, please specify some")
            }
        }

        Bukkit.getPluginManager().registerEvents(this, this)
        Bukkit.getPluginManager().registerEvents(ChatListener(this), this)
        this.getCommand("spy")?.setExecutor(spyChat)
        this.getCommand("mute")?.setExecutor(MuteChannel(this))
        this.getCommand("unmute")?.setExecutor(UnmuteChannel(this))

        val chatList = Array<PlayerChat?>(chats.keys.size) { null }
        chats.keys.forEachIndexed { pos, key -> chatList[pos] = chats[key]!! }
        val nonNullChats = Array<PlayerChat>(chats.keys.size) { pos -> chatList[pos]!! }
        this.getCommand("list_channels")?.setExecutor(ListChannels(nonNullChats))

        // Start a task to register our commands if overwrite is true
        if (configParser.overwrite!!) {
            Bukkit.getPluginManager().registerEvents(ServerLoadListener(this), this)
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent){
        if (event.player.name !in dataParser.playerChannelMap.keys){
            dataParser.setData("Player-Channels.${event.player.name}", configParser.defaultChannel)
        }
    }

}
