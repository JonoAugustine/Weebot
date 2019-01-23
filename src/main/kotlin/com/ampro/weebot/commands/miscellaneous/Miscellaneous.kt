/*
 * Copyright Aquatic Mastery Productions (c) 2018.
 */

package com.ampro.weebot.commands.miscellaneous

import com.ampro.weebot.GENERIC_ERR_MSG
import com.ampro.weebot.commands.*
import com.ampro.weebot.commands.miscellaneous.CmdApiToGetALife.EndPoint.*
import com.ampro.weebot.database.*
import com.ampro.weebot.database.constants.LINK_INVITEBOT
import com.ampro.weebot.extensions.*
import com.ampro.weebot.util.Link
import com.ampro.weebot.util.get
import com.github.kittinunf.fuel.httpGet
import com.jagrosh.jdautilities.command.CommandEvent
import net.dv8tion.jda.core.Permission
import java.time.temporal.ChronoUnit

/**
 * @author Jonathan Augustine
 * @since 1.0
 */
class PingCommand : WeebotCommand("ping", null, arrayOf("pong"), CAT_MISC,
    "", "Checks the bot's latency.", HelpBiConsumerBuilder("Ping ~ Pong", false)
        .setDescription("Checks the bot's latency.").build(), false, cooldown = 10
) {
    override fun execute(event: CommandEvent) {
        STAT.track(this,
                if (event.guild != null) getWeebotOrNew(event.guild) else DAO.GLOBAL_WEEBOT,
                event.author, event.creationTime)
        val r = if (event.getInvocation().toLowerCase() == "pong") "Ping" else "Pong"
        event.reply("$r: ...") { m ->
            val ping = event.message.creationTime.until(m.creationTime, ChronoUnit.MILLIS)
            m.editMessage("$r! :ping_pong: Ping: " + ping + "ms | Websocket: "
                    + event.jda.ping + "ms").queue()
        }
    }
}

/**
 * Sends a link to invite the bot to another server.
 *
 * @author Jonathan Augustine
 * @since 1.0
 */
class CmdInviteLink : WeebotCommand("invitelink", "Invite Link" ,
    arrayOf("ilc", "invite"), CAT_MISC, "", "Get an invite link for Weebot.",
    HelpBiConsumerBuilder("Get an invite link for Weebot")
        .setDescription("[`Or just invite me with this link I guess`]($LINK_INVITEBOT)")
        .setThumbnail(weebotAvatar).build(), cooldown = 360,
    botPerms = arrayOf(Permission.MESSAGE_EMBED_LINKS),
    userPerms = arrayOf(Permission.MESSAGE_EMBED_LINKS)
) {
    override fun execute(event: CommandEvent) {
        STAT.track(this, getWeebotOrNew(event.guild), event.author, event.creationTime)
        makeEmbedBuilder("Invite me to another server!", LINK_INVITEBOT,
            "[`Invite me with dis thing here`]($LINK_INVITEBOT)")
            .setThumbnail(weebotAvatar).build().send(event.channel)
    }
}

/**
 * @author Jonathan Augustine
 * @since 2.2.0
 */
class CmdNameGenerator : WeebotCommand("namegen", "Name Generator", arrayOf("ngc"),
    CAT_UNDER_CONSTRUCTION, "", "Generate a random name", cooldown = 15) {

    private val BASE_URL = "https://uzby.com/api.php" //TODO java rejecting SSL cert

    override fun execute(event: CommandEvent) {
        val result = BASE_URL.httpGet(listOf("min" to 2, "max" to 40)).response()
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Name Generator", """
            ``namegen [-min minLength] [-max maxLength]``
            minLength must be more than 1
            maxLength must be under 41
        """.trimIndent(), false)
            .setAliases(aliases)
            .build()
    }

}

/**
 * @author Jonathan Augustine
 * @since 2.2.0
 */
class CmdApiToGetALife : WeebotCommand("fact", "APGL Facts", emptyArray(),
    CAT_MISC, "", "Get random facts and images from api-to-get-a.life",
    cooldown = 20, cooldownScope = CooldownScope.USER, guildOnly = true) {

    private val BASE_URL = "https://api-to.get-a.life"

    private enum class EndPoint(val endPoint: String) {
        FACT_DOG("/dogfact"), FACT_CAT("/catfact"), FACT_PANDA("/pandafact"),
        IMG_DOG("/dogimg"), IMG_CAT("/catimg"), IMG_PANDA("/pandaimg"),
        IMG_REDPANDA("/redpandaimg"), IMG_BIRD("/birbimg"), IMG_PIKACHU("/pikachuimg");
        override fun toString() = this.endPoint
    }
    private data class Fact(val fact: String)

    override fun execute(event: WeebotCommandEvent) {
        when {
            event.args.isNullOrBlank() -> {
                this.execute(WeebotCommandEvent(event.event,
                    listOf("dog", "cat", "panda").random(), event.bot))
            }
            event.args.matches("(?i)dog") -> {
                (BASE_URL + FACT_DOG).get<Fact>().component1()?.also {
                    makeEmbedBuilder("Doggo Fact", null, it.fact)
                        .setImage((BASE_URL+IMG_DOG).get<Link>().component1()?.link)
                        .build().send(event.channel)
                } ?: event.respondThenDeleteBoth(GENERIC_ERR_MSG)
            }
            event.args.matches("(?i)cat") -> {
                (BASE_URL + FACT_CAT).get<Fact>().component1()?.also {
                    makeEmbedBuilder("Kat Fact", null, it.fact)
                        .setImage((BASE_URL+IMG_CAT).get<Link>().component1()?.link)
                        .build().send(event.channel)
                } ?: event.respondThenDeleteBoth(GENERIC_ERR_MSG)
            }
            event.args.matches("(?i)panda") -> {
                (BASE_URL + FACT_PANDA).get<Fact>().component1()?.also {
                    makeEmbedBuilder("Giant Panda Fact", null, it.fact)
                        .setImage((BASE_URL+IMG_PANDA).get<Link>().component1()?.link)
                        .build().send(event.channel)
                } ?: event.respondThenDeleteBoth(GENERIC_ERR_MSG)
            }
            event.args.matches("(?i)red(panda)?") -> TODO(event)
            event.args.matches("(?i)pika(chu)?") -> TODO(event)
            event.args.matches("(?i)(bir[bd])") -> TODO(event)
        }
    }

    init {
        helpBiConsumer = HelpBiConsumerBuilder("Api To Get a Life", false)
            .setDescription("""
                Get fun facts and/or pictures about...well... cute animals I guess
                ``dog``
                ``cat``
                ``panda``
            """.trimIndent())
            .build()
    }
}
