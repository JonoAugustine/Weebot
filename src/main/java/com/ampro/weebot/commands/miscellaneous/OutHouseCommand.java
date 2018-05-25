package com.ampro.weebot.commands.miscellaneous;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Have the bot respond to your metions while you're AFK but shown as online. <br>
 *     Requires the bot condition {@link Weebot#ACTIVE_PARTICIPATE} set true.
 */
public class OutHouseCommand extends Command {

    /**
     * An instantiable representation of a User's OutHouse.
     */
    public static final class OutHouse implements IPassive {

        final User user;
        long remainingHours;
        final OffsetDateTime startTime;
        final String message;

        public OutHouse(User user, long hours) {
            startTime = OffsetDateTime.now();
            this.remainingHours = hours;
            this.user = user;
            this.message = null;
        }

        public OutHouse(User user, long hours, String message) {
            startTime = OffsetDateTime.now();
            this.remainingHours = hours;
            this.user = user;
            this.message = message;
        }

        @Override
        public void accept(BetterMessageEvent event) {
            this.remainingHours -=
                    ChronoUnit.HOURS.between(startTime, OffsetDateTime.now());
            if (!event.isPrivate()) {
                if(event.getType() == BetterMessageEvent.TYPE.RECIVED && event
                        .getAuthor() == user) {
                    synchronized (Launcher.GLOBAL_WEEBOT) {
                        Launcher.GLOBAL_WEEBOT.getPassives().remove(this);
                    }
                    Member mem = event.getGuild().getMember(this.user);
                    event.reply("*Welcome back " + mem.getEffectiveName() + "*");
                    return;
                } else if(event.mentions(this.user)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("*Sorry, ")
                      .append(event.getGuild().getMember(this.user).getEffectiveName());
                    if (this.message != null)
                        sb.append(" is out ").append(this.message);
                      else
                        sb.append(" is currently unavailable. ");
                      sb.append("Please try mentioning them again ")
                      .append("in ").append(this.remainingHours)
                      .append(" hours. Thank you.*");
                    event.reply(sb.toString());
                    return;
                } else if (this.remainingHours <= 0) {
                    synchronized (Launcher.GLOBAL_WEEBOT) {
                        Launcher.GLOBAL_WEEBOT.getPassives().remove(this);
                    }
                }
            }
        }

    }

    public OutHouseCommand() {
        super(
                "OutHouse",
                new ArrayList<>(Arrays.asList("ohc")),
                "Have the bot respond to anyone who mentions you for the given time.",
                "outhouse [time]",
                false,
                false,
                0,
                false,
                false
        );
    }

    @Override
    public void run(Weebot bot, BetterMessageEvent event) {
        if (this.check(event)) {
            this.execute(bot, event);
        }
    }

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        //ohc [hours] [message here]
        StringBuilder sb = new StringBuilder();
        String[] args = this.cleanArgs(bot, event.getArgs());
        long hours;
        int messageIndex;
        try {
            hours = Long.parseLong(args[1]);
            messageIndex = 2;
        } catch (IndexOutOfBoundsException e) {
            hours = 1;
            messageIndex = 1;
        } catch (NumberFormatException e) {
            hours = 1;
            messageIndex = 1;
        }

        String m = String.join(" ", Arrays.copyOfRange(args, messageIndex, args.length));
        OutHouse oh;
        if (m.isEmpty()) {
            oh = new OutHouse(event.getAuthor(), hours);
        } else {
            oh = new OutHouse(event.getAuthor(), hours, m);
        }
        Launcher.GLOBAL_WEEBOT.getPassives().add(oh);

        event.reply("I will hold down the fort while you're away! :guardsman: ");

    }

    @Override
    public MessageEmbed getEmbedHelp() {
        EmbedBuilder eb = Launcher.getStandardEmbedBuilder();

        eb.setTitle("OutHouse Command")
          .setDescription("Have the bot respond to mentions for you while you're away.")
          .addField("Guid", "[optional]", false)
          .addField("ohc [hours] [afk-message]", "*Alias*: outhouse", false);

        return eb.build();
    }
}
