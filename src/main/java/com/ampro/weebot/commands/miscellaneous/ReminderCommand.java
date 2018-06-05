package com.ampro.weebot.commands.miscellaneous;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

/**
 * Start or stop a {@link Reminder}.
 */
public class ReminderCommand extends Command {

    /**
     * A Reminder that can last up to 1 month.
     */
    public static final class Reminder implements Runnable {

        @Override
        public void run() {

        }

    }

    public ReminderCommand() {
        super(
               "Reminder",
               new String[]{"rc, remindme"},
               "Set a Reminder.",
               "[date] <time>", //TODO
               false,
               false,
               0,
               false,
               false
        );
    }

    private enum ACTION{START, SEE, CANCEL}

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        String[] args = cleanArgs(bot, event);
        ACTION action = parseAction(args[1]);


        //TODO
        switch (action) {
            case START:
                break;
            case SEE:
                break;
            case CANCEL:
                break;
        }

    }

    /**
     * Parse an {@link ACTION} fromt a string.
     * @param arg The string to parse from.
     * @return The corrisponing {@link ACTION}
     *          or {@link ACTION#START} if on eis not found
     */
    private ACTION parseAction(String arg) {
        switch (arg.toLowerCase()) {
            case "seeall":
            case "all":
            case "see":
                return ACTION.SEE;
            case "end":
            case "cancel":
                return ACTION.CANCEL;
            default:
                return ACTION.START;
        }
    }

    @Override
    public MessageEmbed getEmbedHelp() {
        EmbedBuilder eb = Launcher.getStandardEmbedBuilder();
        //TODO
        return eb.build();
    }
}
