package com.ampro.weebot.commands.management;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.properties.Restriction;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.managers.GuildController;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A Command used to restrict the usage of other commands <br>
 *      <em>
 *          I wanted to call it RestrictCommandCommand but that would be dumb
 *      </em>
 *      TODO add to help
 */
public class RestrictCmdCommand extends Command {

    public RestrictCmdCommand() {
        super(
                "restrictcommand",
                new String[]{"rcc", "restrictcmd", "rcmd", "perm"},
                "*Restrict access to a command.*",
                "<action> <command> [command2]... /member, role, textChannel/...",
                true,
                false,
                0,
                false,
                false
        );
        userPermissions = new Permission[]{Permission.ADMINISTRATOR};
    }

    private enum Action {ALLOW, BLOCK, UNLOCK, SEE}

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        String[] args = cleanArgs(bot, event);
        Action action = parseAction(args[1]);
        if (action == null) {
            event.reply("*You need to provide an action.* ```help rcc```");
            return;
        }
        //Add listed commands to a...well a list
        ArrayList<Class<? extends Command>> commandClasses = new ArrayList<>();
        //rcc action [command]
        //Start at spot 3 (index 2)
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            for (Command cmd : Launcher.getCommands()) {
                //For each command, we check if it's even valid, so we can
                //Be 100% sure that the commandClasses list will only have
                //valid classes inside it
                if(cmd.isCommandFor(arg)) {
                    commandClasses.add(cmd.getClass());
                }
            }
        }
        if (!action.equals(Action.SEE) && commandClasses.isEmpty()) {
            event.reply("*You need to provide commandClasses.*```help rcc```");
            return;
        }
        ConcurrentHashMap<Class<? extends Command>, Restriction> restrictions
                = bot.getCommandRestrictions();
        StringBuilder sb = new StringBuilder();
        Guild guild = event.getGuild();

        switch (action) {
            case ALLOW:
                commandClasses.forEach(c -> {
                    Restriction res = restrictions.get(c);
                    res.allow(event.getMessage().getMentionedUsers());
                    res.allow(event.getMessage().getMentionedRoles());
                    res.allow(event.getMessage().getMentionedChannels());
                });
                break;
            case BLOCK:
                commandClasses.forEach(c -> {
                    Restriction res = bot.getCmdRestriction(c);
                    res.block(event.getMessage().getMentionedUsers());
                    res.block(event.getMessage().getMentionedRoles());
                    res.block(event.getMessage().getMentionedChannels());
                });
                break;
            case UNLOCK:
                //unlock [command]...
                commandClasses.forEach(restrictions::remove);
                break;
            case SEE:
                //see [command]...
                EmbedBuilder eb;
                sb.setLength(0);
                if (commandClasses.size() == 1) {
                    //Show a cleaner embed
                    Class<? extends Command> cClass = commandClasses.get(0);
                    Restriction res = restrictions.get(cClass);
                    eb = res.toEmbedBuilder(guild)
                            .setTitle(cClass.getSimpleName() + " Restrictions");
                    event.reply(eb.build());
                    return;
                } else {
                    eb = this.makeEmbed(restrictions, commandClasses, guild, bot);
                    eb.setTitle("Command Restrictions");
                    event.reply(eb.build());
                }
                break;
        }

    }

    /**
     * @param restrictions The restrictions mapped to their commands
     * @param commands The commands to list
     * @param guild The working guild
     * @return An {@link EmbedBuilder} with each field as a command and it's
     *          restrictions, no title or description.
     */
    private EmbedBuilder
    makeEmbed(AbstractMap<Class<? extends Command>, Restriction> restrictions,
              List<Class<? extends Command>> commands, Guild guild, Weebot bot) {
        EmbedBuilder eb = Launcher.getStandardEmbedBuilder();
        StringBuilder sb = new StringBuilder();
        //If the given list is empty, then user every restriction
        List<Class<? extends Command>> list = commands.isEmpty()
                                              ? new ArrayList<>(restrictions.keySet())
                                              : commands;
        list.forEach( c -> {
            Restriction res = bot.getCmdRestriction(c);
            if (!res.getAllowedUsers().isEmpty()) {
                sb.append("Allowed Members:\n");
                res.getAllowedUsers().forEach(u -> {
                    sb.append("\t*")
                      .append(guild.getMemberById(u).getEffectiveName())
                      .append("*\n");
                });
                sb.setLength(0);
            }
            if (!res.getAllowedRoles().isEmpty()) {
                sb.append("Allowed Roles:\n");
                res.getAllowedRoles().forEach(r -> {
                    sb.append("\t*")
                      .append(guild.getRoleById(r).getName())
                      .append("*\n");
                });
                sb.setLength(0);
            }
            if (!res.getAllowedTextChannels().isEmpty()) {
                sb.append("Allowed TextChannels:\n");
                res.getAllowedTextChannels().forEach(tc -> {
                    sb.append("\t*")
                      .append(guild.getTextChannelById(tc).getName())
                      .append("*\n");
                });
                sb.setLength(0);
            }
            if (!res.getBlockedUsers().isEmpty()) {
                sb.append("Blocked Members:\n");
                res.getBlockedUsers().forEach(u -> {
                    sb.append("\t*")
                      .append(guild.getMemberById(u).getEffectiveName())
                      .append("*\n");
                });
                sb.setLength(0);
            }
            if (!res.getBlockedRoles().isEmpty()) {
                sb.append("BlockedRoles:\n");
                res.getBlockedRoles().forEach(r -> {
                    sb.append("\t*")
                      .append(guild.getRoleById(r).getName())
                      .append("*\n");
                });
                sb.setLength(0);
            }
            if (!res.getBlockedTextChannels().isEmpty()) {
                sb.append("Blocked TextChannels:\n");
                res.getBlockedTextChannels().forEach(tc -> {
                    sb.append("\t*")
                      .append(guild.getTextChannelById(tc).getName())
                      .append("*\n");
                });
                sb.setLength(0);
            }
            eb.addField(c.getSimpleName() + " Restrictions", sb.toString(), true);
        });
        return eb;
    }

    /**
     * Parse an {@link Action} form a string.
     * @param arg The string to parse from
     * @return The parsed Action, null if non parsed.
     */
    private static Action parseAction(String arg) {
        switch (arg.toLowerCase()) {
            case "see":
                return Action.SEE;
            case "allow":
                return Action.ALLOW;
            case "block":
                return Action.BLOCK;
            case "unlock":
            case "open":
                return Action.UNLOCK;
            default:
                return null;
        }
    }

    @Override
    public MessageEmbed getEmbedHelp() {
        EmbedBuilder eb = Launcher.getStandardEmbedBuilder();
        //TODO
        return super.getEmbedHelp();
    }
}
