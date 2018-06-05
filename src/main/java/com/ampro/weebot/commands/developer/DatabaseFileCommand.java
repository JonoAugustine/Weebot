package com.ampro.weebot.commands.developer;

import com.ampro.weebot.commands.Command;
import com.ampro.weebot.database.DatabaseManager;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Send the latest Database backup file in Private Message.
 */
public class DatabaseFileCommand extends Command {

    public DatabaseFileCommand() {
        super(
            "DatabaseFile",
            new String[]{"dbf", "wbotfile", "wbotdb"},
            "Get the latest wbot Database backup.",
            "dbf",
            false,
            true,
            0,
            true,
            false
        );
    }

    /**
     * Performs a check then runs the command.
     *
     * @param bot
     *         The {@link Weebot} that called the command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    public void run(Weebot bot, BetterMessageEvent event) {
        if (this.check(event)) {
            this.execute(bot, event);
        }
    }

    /**
     * Performs the action of the command.
     *
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        File out = new File(DatabaseManager.DIR, "databaseBK.wbot");
        if (!out.exists()) {
            event.privateReply("There is no backup file.");
            return;
        }
        String now = OffsetDateTime.now()
                                   .format(DateTimeFormatter
                                                   .ofPattern("dd-MM-yy hh-mm"));
        String name = "Databse  " + now;
        event.privateReply(out, name);
        if (!event.isPrivate())
            event.deleteMessage();
    }
}
