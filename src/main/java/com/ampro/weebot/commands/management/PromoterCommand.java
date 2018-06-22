package com.ampro.weebot.commands.management;

import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Initialize, edit or disable a {@link Promoter} passive.
 * TODO
 */
public class PromoterCommand extends Command {

    /**
     * Automatically assigns members new roles based on member activity.
     */
    public static final class Promoter implements IPassive {

        private boolean dead;

        /** The path along which users are promoted {Role 1 -> Role 2 -> Role 3}*/
        private List<Long> promotionPath;

        /**
         * Initalize a Promoter with a defined promotion path.
         * @param promotionPath
         */
        Promoter(Collection<Role> promotionPath) {
            promotionPath = new ArrayList<>();
            promotionPath.forEach(role -> this.promotionPath.add(role.getIdLong()));
            dead = false;
        }

        @Override
        public void accept(Weebot bot, BetterMessageEvent event) {
            //Use GuildController methods

        }

        @Override
        public boolean dead() {
            return this.dead;
        }
    }


    public PromoterCommand() {
        super(
                "Promoter",
                new String[]{"pc, ranker, globalelete"},
                "*Automatically assigns members new roles based on member activity.*",
                "",
                true,
                false,
                0,
                false,
                false
        );
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.userPermissions = new Permission[]{Permission.MANAGE_ROLES};
    }

    private enum ACTION{INIT, EDIT, DISABLE}

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {

    }

    //parse action

}
