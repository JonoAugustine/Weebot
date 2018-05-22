package com.ampro.weebot.commands.miscellaneous;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import com.sun.org.apache.bcel.internal.generic.LADD;
import net.dv8tion.jda.core.entities.User;

import java.lang.reflect.Member;
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

        public OutHouse(User user, long hours) {
            startTime = OffsetDateTime.now();
            this.user = user;
        }

        @Override
        public void accept(BetterMessageEvent event) {
            this.remainingHours -=
                    ChronoUnit.HOURS.between(startTime, OffsetDateTime.now());
            if (event.getType() == BetterMessageEvent.TYPE.RECIVED
                    && event.getAuthor() == user
                    && !event.isPrivate()
                    || this.remainingHours <= 0)
            {
                this.finish();
                event.reply("*Welcome back " + user.getName() + "*");
                return;

            } else if (event.mentions(this.user)) {
                String timeLeft;
                StringBuilder sb = new StringBuilder();
                sb.append("*Sorry, ").append(user.getName())
                  .append(" is currently unavailable. Please try mentioning them again ")
                  .append("in ").append(this.remainingHours)
                  .append(" hours. Thank you.*");
                event.reply(sb.toString());
                return;
            }
        }

        private void finish() {
            synchronized (Launcher.GLOBAL_WEEBOT) {
                Launcher.GLOBAL_WEEBOT.getPassives().remove(this);
            }
        }
    }

    public OutHouseCommand() {
        super(
                "OutHouse",
                new ArrayList<>(Arrays.asList("oh")),
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

    }
}
