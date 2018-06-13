package com.ampro.weebot.commands.developer;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.ChannelType;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A {@link Runnable} {@link Command} implementation that sends a list of all
 * Discord Guilds currently hosting a Weebot.
 *
 * @author Jonathan Augustine
 */
public class ListGuildsCommand extends Command {

    public ListGuildsCommand() {
        super("ListGuilds"
                , new String[]{"showguilds", "showhosts", "listhosts", "allguilds",
                        "allhosts", "listhomes", "distrobution"}
                , "List all the guilds hosting a Weebot."
                ,""
                , false
                , false
                , 0
                , false
                , false
        );
    }

    /**
     * Runs checks then runs the {@link ListGuildsCommand} in a new
     * {@link Thread}.
     * @param bot The {@link Weebot} that called the command.
     * @param event The {@link BetterMessageEvent} that called the command.
     */
    @Override
    public void run(Weebot bot, BetterMessageEvent event) {
        if (this.check(event)) {
            Thread thread = new Thread(() -> this.execute(bot, event));
            thread.setName(bot.getBotId() + " : ListGuildCommand");
            thread.start();
        }
    }

    /**
     * If the event author is not a Weebot developer, reply in the same channel
     * with a list of all Guilds hosting a Weebot and their Weebot's nickname.
     * <p>
     * If the event author is a Weebot developer, reply in the same channel with
     * a list of all Guilds hosting a Weebot and their Weebot's name, as well
     * as a private message containing more details about the Guild and their
     * bot.
     * @param bot The Weebot who called
     * @param event {@link BetterMessageEvent}
     */
    @SuppressWarnings("unchecked")
    protected void execute(Weebot bot, BetterMessageEvent event) {

        Iterable<Weebot> botIterable = Launcher.getDatabase().getWeebots()
                .values();
        String out;

        if (!Launcher.checkDevID(event.getAuthor().getIdLong())
                || event.getMessageChannel().getType() == ChannelType.TEXT) {

            out = "``` Weeebotful Guilds:\n\n";

            for (Weebot w : botIterable) {
                out = out.concat(w.getGuildName() + "\n\t" + w.getNickname() + "\n\t"
                        + w.getBirthday().format(DateTimeFormatter.ofPattern
                        ("dd/MM/yy HH:mm:ss")) + "\n\n");
            }
            out += "```";
            event.reply(out);
        }

       if (Launcher.checkDevID(event.getAuthor().getIdLong())) {
           out = "```Weeebotful Guilds (Dev):\n\n";
           for (Weebot w : botIterable) {
               out = out.concat(
                       w.getGuildName() + "\n\t"
                       + w.getGuildID() + "\n\t"
                       + w.getBotId() + "\n\t"
                       + w.getNickname() + "\n\t"
                       + w.getCallsign() + "\n\t"
                       + w.getBirthday().format(DateTimeFormatter.ofPattern
                               ("dd/MM/yy HH:mm:ss")) + "\n\t\t"
                       + "\n"
               );
           }
           out += "```";
           event.privateReply(out);
       }

    }

}
