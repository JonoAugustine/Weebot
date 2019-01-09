/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.extensions

import com.ampro.weebot.database.getWeebotOrNew
import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.Command.CooldownScope.USER
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.ChannelType.PRIVATE
import net.dv8tion.jda.core.entities.MessageEmbed.Field
import java.util.concurrent.TimeUnit.SECONDS
import java.util.function.BiConsumer

/** @return the string arguments of the message split into a [List]. Does NOT have the
 * command call in it */
fun CommandEvent.splitArgs(): List<String> = this.args.split("\\s+".toRegex())
    .filter { it.isNotBlank() }

/** @return The string used to invoke this command (i.e. the first string of the message */
fun CommandEvent.getInvocation(): String = this.message.contentStripped
    .removePrefix("w!").removePrefix("\\").split(" ")[0]

/**
 * Send a response to a [CommandEvent] and then delete both messages.
 *
 * @param reason The message to send
 * @param delay The delay in seconds between send & delete (default 10)
 */
fun CommandEvent.respondThenDelete(reason: String, delay: Long = 10L) {
    this.reply("*$reason*") { response ->
        if (this.privateChannel != null) {
            response.delete().reason(reason).queueAfter(delay, SECONDS)
        } else {
            event.message.delete().reason(reason).queueAfter(delay, SECONDS) {
                response.delete().reason(reason).queue()
            }
        }
    }
}

/**
 * Send a response to a [CommandEvent] and then delete both messages.
 *
 * @param reason The message to send
 * @param delay The delay in seconds between send & delete
 */
fun CommandEvent.respondThenDelete(reason: MessageEmbed, delay: Long = 10L) {
    this.reply(reason) { response ->
        if (this.privateChannel != null) {
            response.delete().queueAfter(delay, SECONDS)
        } else {
            event.message.delete().queueAfter(delay, SECONDS) {
                response.delete().queue()
            }
        }
    }
}

/**
 * Delete the [Message] and an event
 *
 * @param reason The message to send
 * @param delay The delay in seconds
 */
fun CommandEvent.delete(delay: Long = 0L) {
    if (!isFromType(PRIVATE)) {
        this.message.delete().queueAfter(delay, SECONDS)
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
abstract class WeebotCommand(name: String, val displayName: String?,
                             aliases: Array<String>, category: Category,
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
        constructor(title: String) : this() { embedBuilder.setTitle(title).addField(
            guide) }
        constructor(title: String, description: String) : this(title) {
            this.description.append(description)
        }
        constructor(title: String, withGuide: Boolean) : this() {
            embedBuilder.setTitle(title)
            if (withGuide) embedBuilder.addField(
                guide) else return
        }

        companion object {
            val guide = Field("Guide",
                        "<required> , [optional], <[at][least][one]..>, /situational/",
                false)
        }

        var description = StringBuilder()
        var sendDM: Boolean = true

        val embedBuilder: EmbedBuilder = strdEmbedBuilder

        fun build(): BiConsumer<CommandEvent, Command> {
            return BiConsumer { event, _ ->
                embedBuilder.setDescription(description.toString())
                if (sendDM) event.replyInDm(embedBuilder.build())
                else event.reply(embedBuilder.build())
            }
        }

        fun build(success: (Message) -> Unit) : BiConsumer<CommandEvent, Command> {
            return BiConsumer { event, _ ->
                embedBuilder.setDescription(description.toString())
                if (sendDM) event.replyInDm(embedBuilder.build(), success)
                else event.reply(embedBuilder.build(), success)
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
        fun addToDesc(string: String) : HelpBiConsumerBuilder {
            this.description.append(string)
            return this
        }

        /**
         * Add a list of command aliases
         * @param aliases
         * @return [HelpBiConsumerBuilder]
         */
        fun setAliases(aliases: Array<String>) : HelpBiConsumerBuilder {
            addField("Aliases", "``${aliases.joinToString(", ")}``")
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

        fun addBlankField(b: Boolean = false): HelpBiConsumerBuilder {
            embedBuilder.addBlankField(b)
            return this
        }
    }

    override fun isAllowed(channel: TextChannel): Boolean {
        return getWeebotOrNew(channel.guild).settings.isAllowed(this, channel)
                && super.isAllowed(channel)
    }

    fun getHelpBiConsumer(): BiConsumer<CommandEvent, Command>? = this.helpBiConsumer

}
