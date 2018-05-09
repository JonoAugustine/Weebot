/*
 * Copyright 2016-2018 John Grosh (jagrosh) & Kaidan Gustave (TheMonitorLizard)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ampro.main.bot.commands;

import com.ampro.main.Launcher;
import com.ampro.main.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <h2>Weebot Commands</h2>
 * <p>The execution of commands issued to a Weebot instance.</p>
 * <p>Commands are stored by Weebots</p>
 * @author Jonathan Augustine
 */
public abstract class Command {

    /** Name of the command, to be called the format: {@code [prefix]<name>}. */
    protected final String name;

    /**
     * The aliases of the command, when calling a command these function identically to calling the
     * {@link com.jagrosh.jdautilities.command.Command#name Command.name}.
     */
    protected final List<String> aliases;

    /** A small help String that summarizes the function of the command. */
    protected final String help;

    /** An arguments format String for the command. */
    protected final String argFormat;

    /**
     * {@code true} if the command may only be used in a {@link net.dv8tion.jda.core.entities.Guild Guild},
     * {@code false} if it may be used in both a Guild and a DM.
     * <br>Default {@code true}.
     */
    protected final boolean guildOnly;

    /**
     * {@code true} if the command may only be used by a User with an ID matching the
     * Owners or any of the CoOwners.
     * <br>Default {@code false}.
     */
    protected final boolean ownerOnly;

    /**
     * A String name of a role required to use this command.
     */
    protected String requiredRole = null;

    /**
     * An {@code int} number of seconds users must wait before using this command again.
     */
    protected final int cooldown;

    /**
     * Any {@link net.dv8tion.jda.core.Permission Permission}s a Member must have to use this command.
     * <br>These are only checked in a {@link net.dv8tion.jda.core.entities.Guild Guild} environment.
     */
    protected Permission[] userPermissions = new Permission[0];

    /**
     * Any {@link net.dv8tion.jda.core.Permission Permission}s the bot must have to use a command.
     * <br>These are only checked in a {@link net.dv8tion.jda.core.entities.Guild Guild} environment.
     */
    protected Permission[] botPermissions = new Permission[0];

    /**
     * {@code true} if this command checks a channel topic for topic-tags.
     * <br>This means that putting {@code {-commandname}}, {@code {-command category}}, {@code {-all}} in a channel topic
     * will cause this command to terminate.
     * <br>Default {@code true}.
     */
    protected boolean usesTopicTags;

    /**
     * The child commands of the command. These are used in the format {@code [prefix]<parent name>
     * <child name>}.
     */
    protected Command[] children;

    /** {@code true} if this command should be hidden from the help. */
    protected final boolean hidden;

    /**
     * The {@link com.jagrosh.jdautilities.command.Command.CooldownScope CooldownScope}
     * of the command. This defines how far of a scope cooldowns have.
     * <br>Default {@link com.jagrosh.jdautilities.command.Command.CooldownScope#USER CooldownScope.USER}.
     */
    protected CooldownScope cooldownScope = CooldownScope.USER;

    private final static String BOT_PERM
            = "%s I need the %s permission in this %s!";
    private final static String USER_PERM
            = "%s You must have the %s permission in this %s to use that!";

    public Command() {
        this.name       = null;
        this.aliases    = null;
        this.help       = null;
        this.argFormat  = null;
        this.guildOnly  = false;
        this.ownerOnly  = false;
        this.cooldown   = 0;
        this.hidden     = false;
    }


    public Command(String name, List<String> aliases, String help
            , String argFormat, boolean guildOnly, boolean ownerOnly
            , int cooldown, boolean hidden) {

        this.name       = name;
        this.aliases    = aliases;
        this.help       = help;
        this.argFormat  = argFormat;
        this.guildOnly  = guildOnly;
        this.ownerOnly  = ownerOnly;
        this.cooldown   = cooldown;
        this.hidden     = hidden;

    }

    /**
     * Performs a check then runs the command.
     * @param bot The {@link Weebot} that called the command.
     * @param event The {@link BetterMessageEvent} that called the command.
     */
    public abstract void run(Weebot bot, BetterMessageEvent event);

    /**
     * Performs the action of the command.
     * @param event The {@link BetterMessageEvent} that called the command.
     */
    protected abstract void execute(BetterMessageEvent event);

    /**
     * Performs the action of the command.
     * @param bot The {@link Weebot} which called this command.
     * @param event The {@link BetterMessageEvent} that called the command.
     */
    protected abstract void execute(Weebot bot, BetterMessageEvent event);

    /**
     * Checks for the Command with the given
     * {@link com.ampro.main.listener.events.BetterMessageEvent} and
     * {@link com.ampro.main.bot.Weebot Weebot} that called it.
     * <br>Will terminate and possibly respond with a failure message if any checks fail.
     *
     * @param  event
     *         The BetterMessageEvent passed to the command.
     */
    protected boolean check(BetterMessageEvent event) {
        //Check Channel

        // child check
        //if(event.getArgs().length != 0) {
        //    String[] parts = Arrays.copyOf(event.getArgs(), 2);
        //   for(Command cmd: children) {
        //        if(cmd.isCommandFor(parts[0])) {
        //        }
        //    }
        //}

        //user permission check

        //If the command is for developer's only, check if the event Author's
        //ID is registered as a developer.
        if (this.ownerOnly) {
            if (!Launcher.getDatabase().getDevelopers()
                    .contains(event.getAuthor().getIdLong()))
            {
                return false;
            }
        }

        //Bot permissions

        //required role check


        //cooldown check

        return true;
    }

    /**
     * Checks if the given input represents this Command
     *
     * @param  input
     *         The input to check
     *
     * @return {@code true} if the input is the name or an alias of the Command
     */
    public boolean isCommandFor(String input) {
        if(this.name.equalsIgnoreCase(input))
            return true;
        for(String alias: this.aliases)
            if(alias.equalsIgnoreCase(input))
                return true;
        return false;
    }

    /**
     * Removes the callsigns (@Weebot, or bot's {@link Weebot#CALLSIGN}) from
     * the args.
     * @param args String array to clean.
     * @return new string array with the command call at index {@code [0]}.
     */
    protected String[] cleanArgs(Weebot bot, String[] args) {
        //Make it an ArrayList b/c easy to work with
        ArrayList<String> temp = new ArrayList<>(Arrays.asList(args));
        if (args[0].startsWith(bot.getCallsign())) {
            //^Check if the command was called with the callsign

            //Split the first index by spaces. This allows for commands to be
            //called w/ or w/o a space between the callsign and the command
            //string (making it so there is only one case to deal with -> fewer
            // if-else statements).
            temp.get(0).split(" ");
            //Then delete the callsign from the first argument
            temp.set(0, temp.get(0).substring(bot.getCallsign().length()));

        } else if (args[0].equals("@" + bot.getNickname())) {
            //^Check if the command was called by mentioning the bot
            //Remove the first argument and replace it with the second
            //(which would be the command arg; e.g. [@Weebot, help])
            temp.remove(0);
            temp.trimToSize();
        }
        return temp.toArray(new String[temp.size()]);
    }

    /**
     * Removes the callsigns (@Weebot, or bot's {@link Weebot#CALLSIGN}) from
     * the args.
     * @param event {@link BetterMessageEvent} to clean the arguments of.
     * @return new string array with the command call at index {@code [0]}.
     */
    protected String[] cleanArgs(Weebot bot, BetterMessageEvent event) {
        return this.cleanArgs(bot, event.getArgs());
    }

    /**
     * Checks whether a command is allowed in a {@link net.dv8tion.jda.core.entities.TextChannel TextChannel}
     * by searching the channel topic for topic tags relating to the command.
     *
     * <p>{-{@link com.jagrosh.jdautilities.command.Command#name name}},
     * {-{@link com.jagrosh.jdautilities.command.Command.Category category name}}, or {-{@code all}}
     * are valid examples of ways that this method would return {@code false} if placed in a channel topic.
     *
     * <p><b>NOTE:</b>Topic tags are <b>case sensitive</b> and proper usage must be in lower case!
     * <br>Also note that setting {@link com.jagrosh.jdautilities.command.Command#usesTopicTags usesTopicTags}
     * to {@code false} will cause this method to always return {@code true}, as the feature would not be applicable
     * in the first place.
     *
     * @param  channel
     *         The TextChannel to test.
     *
     * @return {@code true} if the channel topic doesn't specify any topic-tags that would cause this command
     *         to be cancelled, or if {@code usesTopicTags} has been set to {@code false}.
     */
    public boolean isAllowed(TextChannel channel) {
        if(!usesTopicTags)
            return true;
        if(channel==null)
            return true;
        String topic = channel.getTopic();
        if(topic==null || topic.isEmpty())
            return true;
        topic = topic.toLowerCase();
        String lowerName = name.toLowerCase();
        if(topic.contains("{"+lowerName+"}"))
            return true;
        if(topic.contains("{-"+lowerName+"}"))
            return false;
        return !topic.contains("{-all}");
    }

    /**
     * Gets the {@link com.jagrosh.jdautilities.command.Command#name Command.name} for the Command.
     *
     * @return The name for the Command
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the {@link com.jagrosh.jdautilities.command.Command#help Command.help} for the Command.
     *
     * @return The help for the Command
     */
    public String getHelp() {
        return help;
    }

    /**
     * Gets the {@link com.jagrosh.jdautilities.command.Command#arguments Command.arguments} for the Command.
     *
     * @return The arguments for the Command
     */
    public String getArgFormat() {
        return this.argFormat;
    }

    /**
     * Checks if this Command can only be used in a {@link net.dv8tion.jda.core.entities.Guild Guild}.
     *
     * @return {@code true} if this Command can only be used in a Guild, else {@code false} if it can
     *         be used outside of one
     */
    public boolean isGuildOnly() {
        return guildOnly;
    }

    /**
     * Gets the {@link com.jagrosh.jdautilities.command.Command#requiredRole Command.requiredRole} for the Command.
     *
     * @return The requiredRole for the Command
     */
    public String getRequiredRole() {
        return requiredRole;
    }

    /**
     * Gets the {@link com.jagrosh.jdautilities.command.Command#cooldown Command.cooldown} for the Command.
     *
     * @return The cooldown for the Command
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Gets the {@link com.jagrosh.jdautilities.command.Command#userPermissions Command.userPermissions} for the Command.
     *
     * @return The userPermissions for the Command
     */
    public Permission[] getUserPermissions()
    {
        return userPermissions;
    }

    /**
     * Gets the {@link com.jagrosh.jdautilities.command.Command#botPermissions Command.botPermissions} for the Command.
     *
     * @return The botPermissions for the Command
     */
    public Permission[] getBotPermissions() {
        return botPermissions;
    }

    /**
     * Gets the {@link com.jagrosh.jdautilities.command.Command#aliases Command.aliases} for the Command.
     *
     * @return The aliases for the Command
     */
    public List<String> getAliases() {
        return aliases;
    }

    /**
     * Gets the {@link com.jagrosh.jdautilities.command.Command#children Command.children} for the Command.
     *
     * @return The children for the Command
     */
    public Command[] getChildren() {
        return children;
    }

    /**
     * Checks whether or not this command is an owner only Command.
     *
     * @return {@code true} if the command is an owner command, otherwise {@code false} if it is not
     */
    public boolean isOwnerCommand() {
        return ownerOnly;
    }

    /**
     * Checks whether or not this command should be hidden from the help
     *
     * @return {@code true} if the command should be hidden, otherwise {@code false}
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Gets the proper cooldown key for this Command under the provided
     * {@link com.jagrosh.jdautilities.command.CommandEvent CommanEvent}.
     *
     * @param  event
     *         The CommandEvent to generate the cooldown for.
     *
     * @return A String key to use when applying a cooldown.
     */
    public String getCooldownKey(BetterMessageEvent event) {
        switch (cooldownScope)
        {
            case USER:         return cooldownScope.genKey(name,event.getAuthor().getIdLong());
            case USER_GUILD:   return event.getGuild()!=null ? cooldownScope.genKey(name,event.getAuthor().getIdLong(),event.getGuild().getIdLong()) :
                    CooldownScope.USER_CHANNEL.genKey(name,event.getAuthor().getIdLong(), event.getMessageChannel().getIdLong());
            case USER_CHANNEL: return cooldownScope.genKey(name,event.getAuthor().getIdLong(),event.getMessageChannel().getIdLong());
            case GUILD:        return event.getGuild()!=null ? cooldownScope.genKey(name,event.getGuild().getIdLong()) :
                    CooldownScope.CHANNEL.genKey(name,event.getMessageChannel().getIdLong());
            case CHANNEL:      return cooldownScope.genKey(name,event.getMessageChannel().getIdLong());
            case SHARD:        return event.getJDA().getShardInfo()!=null ? cooldownScope.genKey(name, event.getJDA().getShardInfo().getShardId()) :
                    CooldownScope.GLOBAL.genKey(name, 0);
            case USER_SHARD:   return event.getJDA().getShardInfo()!=null ? cooldownScope.genKey(name,event.getAuthor().getIdLong(),event.getJDA().getShardInfo().getShardId()) :
                    CooldownScope.USER.genKey(name, event.getAuthor().getIdLong());
            case GLOBAL:       return cooldownScope.genKey(name, 0);
            default:           return "";
        }
    }

    /**
     * Gets an error message for this Command under the provided
     * {@link com.jagrosh.jdautilities.command.CommandEvent CommanEvent}.
     *
     * @param  event
     *         The CommandEvent to generate the error message for.
     * @param  remaining
     *         The remaining number of seconds a command is on cooldown for.
     *
     * @return A String error message for this command if {@code remaining > 0},
     *         else {@code null}.
     */
    public String getCooldownError(BetterMessageEvent event, int remaining) {
        if(remaining<=0)
            return null;
        String front = " That command is on cooldown for "+remaining+" more seconds";
        if(cooldownScope.equals(CooldownScope.USER))
            return front+"!";
        else if(cooldownScope.equals(CooldownScope.USER_GUILD) && event.getGuild()==null)
            return front+" "+CooldownScope.USER_CHANNEL.errorSpecification+"!";
        else if(cooldownScope.equals(CooldownScope.GUILD) && event.getGuild()==null)
            return front+" "+CooldownScope.CHANNEL.errorSpecification+"!";
        else
            return front+" "+cooldownScope.errorSpecification+"!";
    }

    public boolean isOwnerOnly() {
        return ownerOnly;
    }

    public void setRequiredRole(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    public void setUserPermissions(Permission[] userPermissions) {
        this.userPermissions = userPermissions;
    }

    public void setBotPermissions(Permission[] botPermissions) {
        this.botPermissions = botPermissions;
    }

    public boolean isUsesTopicTags() {
        return usesTopicTags;
    }

    public void setUsesTopicTags(boolean usesTopicTags) {
        this.usesTopicTags = usesTopicTags;
    }

    /**
     * A series of {@link java.lang.Enum Enum}s used for defining the scope size for a
     * {@link com.jagrosh.jdautilities.command.Command Command}'s cooldown.
     *
     * <p>The purpose for these values is to allow easy, refined, and generally convenient keys
     * for cooldown scopes, allowing a command to remain on cooldown for more than just the user
     * calling it, with no unnecessary abstraction or developer input.
     *
     * Cooldown keys are generated via {@code com.ampro.main.bot.commands.Command#getCooldownKey(CommandEvent)
     * Command#getCooldownKey(CommandEvent)} using 1-2 Snowflake ID's corresponding to the name
     * (IE: {@code USER_CHANNEL} uses the ID's of the User and the Channel from the CommandEvent).
     *
     * <p>However, the issue with generalizing and generating like this is that the command may
     * be called in a non-guild environment, causing errors internally.
     * <br>To prevent this, all of the values that contain "{@code GUILD}" in their name default
     * to their "{@code CHANNEL}" counterparts when commands using them are called outside of a
     * {@link net.dv8tion.jda.core.entities.Guild Guild} environment.
     * <ul>
     *     <li>{@link com.jagrosh.jdautilities.command.Command.CooldownScope#GUILD GUILD} defaults to
     *     {@link com.jagrosh.jdautilities.command.Command.CooldownScope#CHANNEL CHANNEL}.</li>
     *     <li>{@link com.jagrosh.jdautilities.command.Command.CooldownScope#USER_GUILD USER_GUILD} defaults to
     *     {@link com.jagrosh.jdautilities.command.Command.CooldownScope#USER_CHANNEL USER_CHANNEL}.</li>
     * </ul>
     *
     * These are effective across a single instance of JDA, and not multiple
     * ones, save when multiple shards run on a single JVM and under a
     * {@link net.dv8tion.jda.bot.sharding.ShardManager ShardManager}.
     * <br>There is no shard magic, and no guarantees for a 100% "global"
     * cooldown, unless all shards of the bot run under the same ShardManager,
     * and/or via some external system unrelated to JDA-Utilities.
     *
     * @since  1.3
     * @author Kaidan Gustave
     *
     * @see    com.jagrosh.jdautilities.command.Command#cooldownScope Command.cooldownScope
     */
    public enum CooldownScope {
        /**
         * Applies the cooldown to the calling {@link net.dv8tion.jda.core.entities.User User} across all
         * locations on this instance (IE: TextChannels, PrivateChannels, etc).
         *
         * <p>The key for this is generated in the format
         * <ul>
         *     {@code <command-name>|U:<userID>}
         * </ul>
         */
        USER("U:%d",""),

        /**
         * Applies the cooldown to the {@link net.dv8tion.jda.core.entities.MessageChannel MessageChannel} the
         * command is called in.
         *
         * <p>The key for this is generated in the format
         * <ul>
         *     {@code <command-name>|C:<channelID>}
         * </ul>
         */
        CHANNEL("C:%d","in this channel"),

        /**
         * Applies the cooldown to the calling {@link net.dv8tion.jda.core.entities.User User} local to the
         * {@link net.dv8tion.jda.core.entities.MessageChannel MessageChannel} the command is called in.
         *
         * <p>The key for this is generated in the format
         * <ul>
         *     {@code <command-name>|U:<userID>|C:<channelID>}
         * </ul>
         */
        USER_CHANNEL("U:%d|C:%d", "in this channel"),

        /**
         * Applies the cooldown to the {@link net.dv8tion.jda.core.entities.Guild Guild} the command is called in.
         *
         * <p>The key for this is generated in the format
         * <ul>
         *     {@code <command-name>|G:<guildID>}
         * </ul>
         *
         * <p><b>NOTE:</b> This will automatically default back to {@link com.jagrosh.jdautilities.command.Command.CooldownScope#CHANNEL CooldownScope.CHANNEL}
         * when called in a private channel.  This is done in order to prevent internal
         * {@link java.lang.NullPointerException NullPointerException}s from being thrown while generating cooldown keys!
         */
        GUILD("G:%d", "in this server"),

        /**
         * Applies the cooldown to the calling {@link net.dv8tion.jda.core.entities.User User} local to the
         * {@link net.dv8tion.jda.core.entities.Guild Guild} the command is called in.
         *
         * <p>The key for this is generated in the format
         * <ul>
         *     {@code <command-name>|U:<userID>|G:<guildID>}
         * </ul>
         *
         * <p><b>NOTE:</b> This will automatically default back to {@link com.jagrosh.jdautilities.command.Command.CooldownScope#CHANNEL CooldownScope.CHANNEL}
         * when called in a private channel. This is done in order to prevent internal
         * {@link java.lang.NullPointerException NullPointerException}s from being thrown while generating cooldown keys!
         */
        USER_GUILD("U:%d|G:%d", "in this server"),

        /**
         * Applies the cooldown to the calling Shard the command is called on.
         *
         * <p>The key for this is generated in the format
         * <ul>
         *     {@code <command-name>|S:<shardID>}
         * </ul>
         *
         * <p><b>NOTE:</b> This will automatically default back to {@link com.jagrosh.jdautilities.command.Command.CooldownScope#GLOBAL CooldownScope.GLOBAL}
         * when {@link net.dv8tion.jda.core.JDA#getShardInfo() JDA#getShardInfo()} returns {@code null}.
         * This is done in order to prevent internal {@link java.lang.NullPointerException NullPointerException}s
         * from being thrown while generating cooldown keys!
         */
        SHARD("S:%d", "on this shard"),

        /**
         * Applies the cooldown to the calling {@link net.dv8tion.jda.core.entities.User User} on the Shard
         * the command is called on.
         *
         * <p>The key for this is generated in the format
         * <ul>
         *     {@code <command-name>|U:<userID>|S:<shardID>}
         * </ul>
         *
         * <p><b>NOTE:</b> This will automatically default back to {@link com.jagrosh.jdautilities.command.Command.CooldownScope#USER CooldownScope.USER}
         * when {@link net.dv8tion.jda.core.JDA#getShardInfo() JDA#getShardInfo()} returns {@code null}.
         * This is done in order to prevent internal {@link java.lang.NullPointerException NullPointerException}s
         * from being thrown while generating cooldown keys!
         */
        USER_SHARD("U:%d|S:%d", "on this shard"),

        /**
         * Applies this cooldown globally.
         *
         * <p>As this implies: the command will be unusable on the instance of JDA in all types of
         * {@link net.dv8tion.jda.core.entities.MessageChannel MessageChannel}s until the cooldown has ended.
         *
         * <p>The key for this is {@code <command-name>|globally}
         */
        GLOBAL("Global", "globally");

        private final String format;
        final String errorSpecification;

        CooldownScope(String format, String errorSpecification)
        {
            this.format = format;
            this.errorSpecification = errorSpecification;
        }

        String genKey(String name, long id)
        {
            return genKey(name, id, -1);
        }

        String genKey(String name, long idOne, long idTwo)
        {
            if(this.equals(GLOBAL))
                return name+"|"+format;
            else if(idTwo==-1)
                return name+"|"+String.format(format,idOne);
            else return name+"|"+String.format(format,idOne,idTwo);
        }
    }
}
