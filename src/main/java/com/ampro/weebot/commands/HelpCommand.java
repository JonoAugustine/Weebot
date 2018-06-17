package com.ampro.weebot.commands;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.management.ManageSettingsCommand;
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

/**
 * A {@link Command} to send information and help to the use requesting it.
 *
 * @author Jonathan Augustine
 */
public class HelpCommand extends Command {

    private static MessageEmbed EmbedHelp;

    public HelpCommand() {
        super(
                "Help"
                , new String[]{"helpme", "showhelp"}
                , "Show how to interact with me."
                , "<help> [command]"
                , false
                , false
                , 0
                , false
                , false
        );
    }

    /**
     * Reads the command and arguments given and responds with the requested
     * information or help.
     *
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        /** Message arguments cleansed of the callsign or bot mention */
        String[] args = cleanArgs(bot, event);

        //If the only argument is the command invoke
        if (args.length == 1) {
            this.genericHelp(bot, event);
        } else {
            for (Command c : Launcher.getCommands()) {
                if(c.isCommandFor(args[1])) {
                    MessageEmbed eb = c.getEmbedHelp();
                    if (eb == null) {
                        event.privateReply(c.getHelp());
                    } else {
                        event.privateReply(c.getEmbedHelp());
                    }
                    event.deleteMessage();
                    return;
                }
            }
        }

    }

    /**
     * Send the generic help information about Weebot.
     * @param bot The bot who called.
     * @param event The event that invoked this.
     */
    private void genericHelp(Weebot bot, BetterMessageEvent event) {

        event.privateReply(
                Launcher.getCommand(ManageSettingsCommand.class).getEmbedHelp()
        );

        if (EmbedHelp == null) {

            EmbedBuilder eb = Launcher.getStandardEmbedBuilder();
            StringBuilder sb = new StringBuilder();

            eb.setTitle("Weeb(B)ot Features");
            sb.append("Use ' help <feature_command> ' for a list of commands\n").append
                    ("(*The feature's command is shown as 'Feature (command)'*).");
            eb.setDescription(sb.toString());
            sb.setLength(0);
            sb.append(
                    "Custom textChannel moderation with server-wide and channel-specific")
              .append(" word banning, auto-kick and auto-ban at custom infraction limits")
              .append(" (X-strikes your out), exempt roles or members from all rules, " +
                              "").append("and see member infraction records.");
            eb.addField("AutoAdmin (aac)", sb.toString(), false);
            sb.setLength(0);
            sb.append("Write and edit server Note Pads\n").append("Note Pads can be locked to specific")
              .append("roles, members, and text channels.");
            eb.addField("Note Pads (notes)", sb.toString(), true);
            sb.setLength(0);
            sb.append("Play a game of Cards Against Humanity and ").append("make custom decks with user-made cards.\n")
              .append("(*Official decks up to Expansion 3*)");
            eb.addField("Cards Against Humanity (cah)", sb.toString(), true);
            sb.setLength(0);
            eb.addField("Secrete Phrase Game", "(*Under construction*)", true)
              .addField("Reminders (rem)", "Set a reminder for\nup to 30 days.", true)
              .addField("Calculator (calc)", "do...math *and stuff*", true);
            sb.append("Delete the message after a\ngiven amount of seconds").append("\n(30 sec by default)");
            eb.addField("Self-destruct messages (sdc)", sb.toString(), true).addField("OutHouse/LunchBreak (ohc)",
                                                                                      "I will respond to any user who\nmentions you on your behalf.",
                                                                                      true
            ).addField("Bot Invite Link (ilc)",
                       "Get a link to invite me\nto another server", true
            ).addField("List all Weebot guilds (lgc)",
                       "See everywhere I live...\nthat's not creepy at all...", true
            ).addField("Weebot Development Suggestions (sugg)",
                       "Submit feature suggestions for Weebot directly from Discord",
                       false
            ).addField("Ping (ping)", "pong", true);

            EmbedHelp = eb.build();
        }
        event.privateReply(EmbedHelp);

    }

    @Override
    public String getHelp() {
        return "Help Help...wait *what?*";
    }

}
