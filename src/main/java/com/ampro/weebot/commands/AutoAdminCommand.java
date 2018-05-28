package com.ampro.weebot.commands;

import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.Permission;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A Managemnet interface for changing the bot's admin rules and capabilities
 * TODO...all of it?
 */
public class AutoAdminCommand extends Command {


    /**
     * The administrative settings held by the Weebot, e.g. :<br>
     *     Channel-Banned words, infraction limits,
     *     banning or kicking threshold, exempted Roles (IDs)
     */
    public static final class AutoAdmin implements IPassive {


        @Override
        public void accept(BetterMessageEvent event) {
            //TODO Scanning
        }

    }

    public AutoAdminCommand() {
        super(
                "AutoAdmin",
                new ArrayList<>(Arrays.asList("aac", "adminbot", "botadmin")),
                "Control the Bot's admin capabilities and rules.",
                null,
                true,
                false,
                0,
                false,
                false
        );
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {

    }

}
