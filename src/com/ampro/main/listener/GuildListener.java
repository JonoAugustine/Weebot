/**
 *
 */

package com.ampro.main.listener;

import com.ampro.main.Launcher;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageEmbedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * The redistribution center for all incoming Guild events.
 *
 * @author Jonathan Augustine
 */
public class GuildListener extends ListenerAdapter {

	@Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		//Ignore Bots
		if (event.getAuthor().isBot()) return;

        //Get the proper bot
        Launcher.getGuilds().get(event.getGuild()).read(event.getMessage());

	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		Launcher.updateServers(event.getGuild());
	}

	@Override
	public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
	}

	@Override
	public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
	}

	@Override
	public void onGuildMessageEmbed(GuildMessageEmbedEvent event) {
	}

	@Override
	public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
	}

	@Override
	public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
	}

	@Override
	public void onGuildMessageReactionRemoveAll(GuildMessageReactionRemoveAllEvent event) {
	}
}
