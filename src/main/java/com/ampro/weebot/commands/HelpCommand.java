package com.ampro.weebot.commands;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A {@link Command} to send information and help to the use requesting it.
 *
 * @author Jonathan Augustine
 */
public class HelpCommand extends Command {

    public HelpCommand() {
        super(
                "Help"
                , new ArrayList<>(
                        Arrays.asList("helpme", "showhelp")
                )
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
     * Performs a check then runs the command in a new {@link Thread}
     *
     * @param bot
     *         The {@link Weebot} that called the command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    public void run(Weebot bot, BetterMessageEvent event) {
        if(this.check(event)) {
            Thread thread = new Thread(() -> this.execute(bot, event));
            thread.setName(bot.getBotId() + " : HelpCommand");
            thread.start();
        }
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
        String[] args = this.cleanArgs(bot, event);

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

        EmbedBuilder eb = new EmbedBuilder();
        StringBuilder sb = new StringBuilder();

        eb.setColor(new Color(0x31FF00))
          .setAuthor("Weebot", null, Launcher.getJda().getSelfUser().getAvatarUrl())
          .setThumbnail(Launcher.getJda().getSelfUser().getAvatarUrl())
          .setFooter("Run by Weebot", Launcher.getJda().getSelfUser().getAvatarUrl());

        eb.setTitle("Weeb(B)ot Settings")
          .addField("Change My in-Sever NickName",
                    "setname <new nickname>\n*Aliases*: nickname, changename",
                    false);

        sb.append("callw <new_callsign>")
          .append("*Aliases*: callsign, callwith, prefix")
          .append("*Note*: The prefix must be under 4 characters long");
        eb.addField("Change my Callsign/Prefix", sb.toString(), false);
        sb.setLength(0);

        eb.addField("(Dis)Allow Bot to Use Explicit Language",
                    "expl on/off\n*Aliases:*explicit, vulgar, pottymouth",
                    false)
          .addField("(Dis)Allow the bot to use NSFW commands",
                    "nsfw on/off\n*Alias*: naughty",
                    false)
          .addField("Server-wide word bans", "(*Under construction*)", false)
          .addField("(Dis)Allow the bot to respond to actions not directed to it",
                "(*Under construction*)", false);

        event.privateReply(eb.build());

        sb.setLength(0);
        eb.clearFields();


        eb.setTitle("Weeb(B)ot Features");
        sb.append("Use ' help <feature_command> ' for a list of commands\n")
          .append("(*The feature's command is shown as 'Feature (command)'.");
        eb.setDescription(sb.toString());
        sb.setLength(0);

        sb.append("Write and edit server Note Pads\n")
          .append("Note Pads can be locked to specific")
          .append("roles, members, and text channels.");
        eb.addField("Note Pads (notes)", sb.toString(), false);
        sb.setLength(0);

        sb.append("Play a game of Cards Against Humanity and ")
          .append("make custom decks with user-made cards.\n")
          .append("(*Official decks up to Expansion 3*)");
        eb.addField("Cards Against Humanity (cah)", sb.toString(), false);
        sb.setLength(0);

        eb.addField("Secrete Phrase Game", "(*Under construction*)", false)
          .addField("Reminders (rem)","(*Under construction*)", false);

        sb.append("Delete the message after a given amount of seconds")
          .append(" (30 sec by default)");
        eb.addField("Self-destruct messages (sdc)", sb.toString(), false)
          .addField("List all guilds hosting a Weebot (listguilds)",
                    "See everywhere I live...that's not creepy at all...", false)
          .addField("Ping (ping)", "pong", false);

        event.privateReply(eb.build());

    }

    /**
     * Send a list of all the {@link Command}s Weebots can perform
     * and how to use them.
     * @param bot The bot who called
     * @param event The event that invoked
     */
    private void commandsHelp(Weebot bot, BetterMessageEvent event) {
        //TODO commandsHelp
        String out = "```" + bot.getNickname() + " Commands:\n";
        out += "Sorry, this command is still under construction.\n";
        out += "```";
        event.reply(out);
    }

    @Override
    public String getHelp() {
        return "Help Help...wait what?";
    }

}
