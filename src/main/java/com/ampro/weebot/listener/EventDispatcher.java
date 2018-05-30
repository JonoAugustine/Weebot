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

package com.ampro.weebot.listener;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterEvent;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
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
    public void onMessageReceived(MessageReceivedEvent event) {
        event.getChannel().getMessageById(
                event.getMessageId()
        ).queue(
                message -> {
                    User author = message.getAuthor();
                    if (!author.isBot() && author != Launcher.getJda().getSelfUser())
                    {
                        BetterMessageEvent bme;
                        try {
                            bme = new BetterMessageEvent(event, author);
                        } catch (BetterEvent.InvalidAuthorException e) {
                            System.err.println("Is self");
                            return;
                        } catch (BetterEvent.InvalidEventException e) {
                            System.err.println("Invalid Event");
                            return;
                        }
                        try {
                            //Get the proper bot and hand off the event
                            Guild g = event.getGuild();
                            if (g != null) {
                                Launcher.getDatabase()
                                        .getBot(event.getGuild().getIdLong())
                                        .readEvent(bme);
                            } else {
                                //If the guild is not found, hand off to the private bot.
                                Launcher.getDatabase().getBot(0L).readEvent(bme);
                            }
                        } catch(ClassCastException e) {
                            System.err.println("Failed to cast to Weebot.");
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        event.getChannel().getMessageById(
                event.getMessageId()
        ).queue(
                message -> {
                    User author = message.getAuthor();
                    if (!author.isBot() && author != Launcher.getJda().getSelfUser())
                    {
                        BetterMessageEvent bme;
                        try {
                            bme = new BetterMessageEvent(event, author);
                        } catch (BetterEvent.InvalidAuthorException e) {
                            System.err.println("Is self");
                            return;
                        } catch (BetterEvent.InvalidEventException e) {
                            System.err.println("Invalid Event");
                            return;
                        }
                        try {
                            //Get the proper bot and hand off the event
                            Guild g = event.getGuild();
                            if (g != null) {
                                Launcher.getDatabase()
                                        .getBot(event.getGuild().getIdLong())
                                        .readEvent(bme);
                            } else {
                                //If the guild is not found, hand off to the private bot.
                                Launcher.getDatabase().getBot(0L).readEvent(bme);
                            }
                        } catch(ClassCastException e) {
                            System.err.println("Failed to cast to Weebot.");
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Launcher.getDatabase().addBot(new Weebot(event.getGuild()));
        event.getGuild().getDefaultChannel().sendMessage(
                "Welcome, me! \n(call me with ``<>``)"
        ).queue();
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        Launcher.getDatabase().removeBot(event.getGuild().getIdLong());
    }

}
