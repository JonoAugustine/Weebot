package com.ampro.weebot.commands

import com.ampro.weebot.commands.`fun`.CustomMemeCommand
import com.ampro.weebot.commands.developer.*
import com.ampro.weebot.commands.games.SecretePhraseCommand
import com.ampro.weebot.commands.games.cardgame.CardsAgainstHumanityCommand
import com.ampro.weebot.commands.management.*
import com.ampro.weebot.commands.miscellaneous.*
import com.ampro.weebot.commands.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/** A map of all commands by KClass */
val COMMANDS = ConcurrentHashMap<KClass<out Command>, Command>().also {
    it.putAll(mutableMapOf(
            HelpCommand::class          to HelpCommand(),
            ShutdownCommand::class      to ShutdownCommand(),
            DatabaseFileCommand::class  to DatabaseFileCommand(),
            AutoAdminCommand::class     to AutoAdminCommand(),
            ManageSettingsCommand::class    to ManageSettingsCommand(),
            ListGuildsCommand::class    to ListGuildsCommand(),
            PingCommand::class          to PingCommand(),
            SpamCommand::class          to SpamCommand(),
            NotePadCommand::class       to NotePadCommand(),
            SelfDestructMessageCommand::class   to SelfDestructMessageCommand(),
            SecretePhraseCommand::class     to SecretePhraseCommand(),
            WeebotSuggestionCommand::class  to WeebotSuggestionCommand(),
            CardsAgainstHumanityCommand::class  to CardsAgainstHumanityCommand(),
            OutHouseCommand::class      to OutHouseCommand(),
            ChatbotCommand::class       to ChatbotCommand(),
            CalculatorCommand::class    to CalculatorCommand(),
            ReminderCommand::class      to ReminderCommand(),
            InviteLinkCommand::class    to InviteLinkCommand(),
            CustomMemeCommand::class    to CustomMemeCommand(),
            RestrictCmdCommand::class   to RestrictCmdCommand()
    ))
}

/**
 * Get a [Command] from the available list of [COMMANDS].
 *
 * @param klass The class of the [Command].
 * @return  The [Command] that was requested.
 */
fun getCommand(klass: KClass<out Command>): Command = COMMANDS[klass]!!

fun Command.getIntstance() = getCommand(this::class)
