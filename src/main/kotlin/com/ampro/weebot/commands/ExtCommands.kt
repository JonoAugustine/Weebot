/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands

import com.ampro.weebot.bot.Weebot
import com.ampro.weebot.database.DAO
import com.ampro.weebot.database.getWeebotOrNew
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER
import com.jagrosh.jdautilities.command.CommandEvent
import kotlinx.coroutines.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.MessageEmbed.Field
import java.util.function.BiConsumer

/** @return the string arguments of the message split into a [List]. Does NOT have the
 * command call in it */
fun CommandEvent.splitArgs(): List<String> = this.args.split("\\s+".toRegex())

/** @return The string used to invoke this command (i.e. the first string of the message */
fun CommandEvent.getInvocation(): String = this.message.contentStripped
    .removePrefix("w!").removePrefix("\\").split(" ")[0]

/**
 * Send a response to a [CommandEvent] and then delete both messages.
 *
 * @param reason The message to send
 * @param delay The delay in seconds between send & delete
 */
fun CommandEvent.deleteWithResponse(reason: String, delay: Int = 10) {
    this.reply("*$reason*") {
        GlobalScope.launch (Dispatchers.Default) {
            delay(delay * 1000L)
            event.message.delete().reason(reason).queue()
            it.delete().reason(reason).queue()
        }
    }
}


/**
 * A wrapper class for [Command] holding functions used by Weebots
 *
 * @param name Name of the Command
 * @param aliases Alternative names for the command
 * @param
 *
 * @author Jonathan Augustine
 * @since 2.0
 */
abstract class WeebotCommand(name: String, aliases: Array<String>, category: Category,
                             arguments: String, help: String,
                             helpBiConsumer: BiConsumer<CommandEvent, Command>? = null,
                             guildOnly: Boolean = false, ownerOnly: Boolean = false,
                             hidden: Boolean = false, useTopicTags: Boolean = true,
                             children: Array<out Command>? = emptyArray(),
                             requiredRole: String? = null, cooldown: Int = 0,
                             cooldownScope: CooldownScope = USER,
                             userPerms: Array<Permission> = emptyArray(),
                             botPerms: Array<Permission> = emptyArray()
) : Command() {

    init {
        super.name = name
        super.aliases = aliases
        super.help = help
        super.helpBiConsumer = helpBiConsumer
        super.category = category
        super.arguments = arguments
        super.guildOnly = guildOnly
        super.requiredRole = requiredRole
        super.ownerCommand = ownerOnly
        super.cooldown = cooldown
        super.userPermissions = userPerms
        super.botPermissions = botPerms
        super.children = children
    }

    /**
     * A Builder class to make making Help [BiConsumer] embeds a lot simpler
     *
     * @param title The title of the embed
     * @param description
     * @param fields Each successive field in order
     * @param sendDM Whether to send it in a private DM (default = true)
     * @param thumbnailURL default = blank
     * @param imageURL default = blank
     * @author Jonathan Augustine
     * @since 2.0
     */
    open class HelpBiConsumerBuilder() {
        constructor(title: String) : this() { embedBuilder.setTitle(title) }

        companion object {
            val guide = Field("Guide", "<required> , [optional], /situational/", false)
        }

        var description = StringBuilder()
        var sendDM: Boolean = true

        val embedBuilder: EmbedBuilder = EmbedBuilder().addField(guide)

        fun build(): BiConsumer<CommandEvent, Command> {
            return BiConsumer { event, command ->
                embedBuilder.setDescription(description.toString())

                if (sendDM) event.replyInDm(embedBuilder.build())
                else event.reply(embedBuilder.build())
            }
        }

        fun setTitle(title: String, url: String = "") : HelpBiConsumerBuilder {
            embedBuilder.setTitle(title, url)
            return this
        }

        fun setDescription(desc: String) : HelpBiConsumerBuilder {
            this.description = StringBuilder(desc)
            return this
        }

        /** Append a string to the description */
        fun appendDesc(string: String) : HelpBiConsumerBuilder {
            this.description.append(string)
            return this
        }

        fun sendDM(bool: Boolean) : HelpBiConsumerBuilder {
            this.sendDM = bool
            return this
        }

        fun addField(header: String, content: String, inline: Boolean = false)
                : HelpBiConsumerBuilder {
            embedBuilder.addField(Field(header, content, inline))
            return this
        }

        fun addField(field: Field) : HelpBiConsumerBuilder {
            embedBuilder.addField(field)
            return this
        }

        fun setThumbnail(url: String) : HelpBiConsumerBuilder {
            embedBuilder.setThumbnail(url)
            return this
        }

        fun setImage(url: String) : HelpBiConsumerBuilder {
            embedBuilder.setImage(url)
            return this
        }
    }

    /**
     * Send Tracking data to Dao.
     *
     * @param weebot
     * @param event
     */
    fun track(weebot: Weebot, event: CommandEvent) {
        val bot: Weebot = if (event.privateChannel != null) {
            DAO.GLOBAL_WEEBOT
        } else {
            getWeebotOrNew(event.guild?.idLong ?: -1L)
        }
        if (bot.settings.trackingEnabled) {
            //TODO()
        }
    }

    open fun getHelpBiConsumer() = this.helpBiConsumer

}
