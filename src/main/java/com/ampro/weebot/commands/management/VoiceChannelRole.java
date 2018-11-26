package com.ampro.weebot.commands.management;

import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.Permission;

/**
 * On user join, creates a {@link net.dv8tion.jda.core.entities.Role} named after
 * a voice channel and assigns the Role to said user. On user leave, the Role is
 * removed. Once the channel is empty, the Role is deleted.
 *
 * @author Jonathan Augustine
 * @since 1.1
 */
public class VoiceChannelRole extends Command {

     /*
     * On user join, creates a {@link net.dv8tion.jda.core.entities.Role} named after
     * a voice channel and assigns the Role to said user. On user leave, the Role is
     * removed. Once the channel is empty, the Role is deleted.
     */
    public static final class RoleWatcher implements IPassive {



        @Override
        public void accept(Weebot bot, BetterMessageEvent event) {
            event.getGuild().getVoiceChannels().forEach(it -> {
                it.getMembers()
            });
        }


        @Override
        public boolean dead() {
            return false;
        }
    }

    protected VoiceChannelRole() {
        super("VoiceChannelRole", new String[]{"vcrc", "vrc", "vcmc"},
              "On user join, creates a {@link net.dv8tion.jda.core.entities.Role} named after\n"
                      + " * a voice channel and assigns the Role to said user. On user leave, the Role is\n"
                      + " * removed. Once the channel is empty, the Role is deleted.",
              "", true, false, 0, false, false);
        this.userPermissions = new Permission[] {Permission.MANAGE_ROLES};
    }

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {

    }


}
