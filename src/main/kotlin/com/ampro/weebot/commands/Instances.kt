package com.ampro.weebot.commands

import com.ampro.weebot.commands.`fun`.CustomMemeCommand
import com.ampro.weebot.commands.developer.DatabaseFileCommand
import com.ampro.weebot.commands.developer.ListGuildsCommand
import com.ampro.weebot.commands.developer.ShutdownCommand
import com.ampro.weebot.commands.developer.WeebotSuggestionCommand
import com.ampro.weebot.commands.games.SecretePhraseCommand
import com.ampro.weebot.commands.games.cardgame.CardsAgainstHumanityCommand
import com.ampro.weebot.commands.management.AutoAdminCommand
import com.ampro.weebot.commands.management.ManageSettingsCommand
import com.ampro.weebot.commands.management.RestrictCmdCommand
import com.ampro.weebot.commands.miscellaneous.InviteLinkCommand
import com.ampro.weebot.commands.miscellaneous.PingCommand
import com.ampro.weebot.commands.miscellaneous.SpamCommand
import com.ampro.weebot.commands.util.*
import java.util.*

//TODO change to map
val COMMANDS = ArrayList(Arrays.asList(
        HelpCommand(), ShutdownCommand(), DatabaseFileCommand(),
        AutoAdminCommand(), ManageSettingsCommand(),
        ListGuildsCommand(), PingCommand(), SpamCommand(),
        NotePadCommand(), SelfDestructMessageCommand(),
        SecretePhraseCommand(), WeebotSuggestionCommand(),
        CardsAgainstHumanityCommand(), OutHouseCommand(),
        ChatbotCommand(), CalculatorCommand(),
        ReminderCommand(), InviteLinkCommand(),
        CustomMemeCommand(), RestrictCmdCommand()
))

/**
 * Get a [Command] from the available list.
 * @param cl The class of the [Command].
 * @return  The [Command] that was requested.
 * null if not found.
 */
fun getCommand(cl: Class<out Command>): Command? {
    for (c in COMMANDS) {
        if (c.javaClass == cl) {
            return c
        }
    }
    return null
}
