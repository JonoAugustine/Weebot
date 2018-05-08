package com.ampro.main.bot.commands;

import com.ampro.main.Launcher;
import com.ampro.main.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.ChannelType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A {@link Runnable} {@link Command} implementation that sends a list of all
 * Discord Guilds currently hosting a Weebot.
 *
 * @author Jonathan Augustine
 */
public class ListGuildsCommand extends Command {

    public ListGuildsCommand() {
        super("ListGuilds"
                , new ArrayList<>(Arrays.asList(
                        "showguilds", "showhosts", "listhosts", "allguilds"
                        , "allhosts"
                ))
                , "List all the guilds hosting a Weebot."
                ,""
                , false
                , false
                , 0
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
     * @param event {@link com.ampro.main.listener.events.BetterMessageEvent}
     */
    @Override
    protected void execute(BetterMessageEvent event) {

        Iterator<Weebot> botIterator = Launcher.getDatabase().getWeebots()
                .values().iterator();
        String out;

        if (!Launcher.getDatabase().getDevelopers()
                .contains(event.getAuthor().getIdLong())
                || event.getMessageChannel().getType() == ChannelType.TEXT) {

            out = "``` Weeebotful Guilds:\n\n";

            while (botIterator.hasNext()) {
                Weebot curr = (Weebot) botIterator.next();
                out += curr.getGuildName() + "\n\t";
                out += curr.getNickname() + "\n";
                //TODO out += curr.getBirthday();
            }
            out += "```";
            event.reply(out);
        }
       if (Launcher.getDatabase().getDevelopers()
               .contains(event.getAuthor().getIdLong())) {

           botIterator = Launcher.getDatabase().getWeebots()
                   .values().iterator();

           out = "```Weeebotful Guilds (Dev):\n\n";
           while (botIterator.hasNext()) {
               Weebot curr = botIterator.next();
               out += curr.getGuildName() + "\n\t";
               out += curr.getGuildID() + "\n\t";
               out += curr.getBotId()   + "\n\t";
               out += curr.getNickname()   + "\n\t";
               out += curr.getCallsign()    + "\n";
               //TODO out += curr.getBirthday();
           }
           out += "```";
           event.privateReply(out);
       }
    }

    /**
     * Run this bot's own implementation of
     * {@link ListGuildsCommand#execute(BetterMessageEvent)}
     * @param bot The {@link Weebot} which called this command.
     * @param event The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) { this.execute(event); }

}
