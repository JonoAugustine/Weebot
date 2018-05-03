/**
 *  Copyright 2018 Jonathan Augustine
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

package com.ampro.main.listener;

import com.ampro.main.Launcher;
import com.ampro.main.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageEmbedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageEmbedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageUpdateEvent;
import net.dv8tion.jda.core.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.priv.react.PrivateMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Event Dispatcher is a global Listener that distributes events
 * to corresponding Weebots.
 *
 * @author Jonathan Augustine
 */
public class EventDispatcher extends ListenerAdapter {

    //Guild Events

    @Override
    public void onGenericMessage(GenericMessageEvent event) {
        try {
            //Get the proper bot and hand off the event
            ((Weebot) Launcher.getDatabase().getWeebots()
                    .get(event.getGuild().getIdLong()))
                    .readEvent(new BetterMessageEvent(event));
        } catch (Exception e) {
            //Ignore ourself
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Launcher.getDatabase().addBot(new Weebot(event.getGuild()));
        event.getGuild().getDefaultChannel().sendMessage(
                "Welcome, me! \n(call me with ``<>``)"
        ).queue();
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {

    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        //Ignore ourself
        if (event.getAuthor().equals(Launcher.getJDA().getSelfUser()))
            return;
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
    }

    @Override
    public void onGuildMessageEmbed(GuildMessageEmbedEvent event) {
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
    }

    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
    }

    @Override
    public void onGuildMessageReactionRemoveAll(GuildMessageReactionRemoveAllEvent event) {
    }

    //Private Events

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
    }

    @Override
    public void onPrivateMessageUpdate(PrivateMessageUpdateEvent event) {
    }

    @Override
    public void onPrivateMessageDelete(PrivateMessageDeleteEvent event) {
    }

    @Override
    public void onPrivateMessageEmbed(PrivateMessageEmbedEvent event) {
    }

    @Override
    public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
    }

    @Override
    public void onPrivateMessageReactionRemove(PrivateMessageReactionRemoveEvent event) {
    }

}
