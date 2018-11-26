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
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.listener.events.BetterEvent;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent;
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.core.events.channel.voice.update.*;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.events.role.RoleCreateEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
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

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        super.onGuildVoiceUpdate(event);
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        super.onGuildVoiceJoin(event);
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        super.onGuildVoiceMove(event);
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        super.onGuildVoiceLeave(event);
    }

    @Override
    public void onGuildVoiceMute(GuildVoiceMuteEvent event) {
        super.onGuildVoiceMute(event);
    }

    @Override
    public void onGuildVoiceDeafen(GuildVoiceDeafenEvent event) {
        super.onGuildVoiceDeafen(event);
    }

    @Override
    public void onGuildVoiceGuildMute(GuildVoiceGuildMuteEvent event) {
        super.onGuildVoiceGuildMute(event);
    }

    @Override
    public void onGuildVoiceGuildDeafen(GuildVoiceGuildDeafenEvent event) {
        super.onGuildVoiceGuildDeafen(event);
    }

    @Override
    public void onGuildVoiceSelfMute(GuildVoiceSelfMuteEvent event) {
        super.onGuildVoiceSelfMute(event);
    }

    @Override
    public void onGuildVoiceSelfDeafen(GuildVoiceSelfDeafenEvent event) {
        super.onGuildVoiceSelfDeafen(event);
    }

    @Override
    public void onGuildVoiceSuppress(GuildVoiceSuppressEvent event) {
        super.onGuildVoiceSuppress(event);
    }

    @Override
    public void onVoiceChannelDelete(VoiceChannelDeleteEvent event) {
        super.onVoiceChannelDelete(event);
    }

    @Override
    public void onVoiceChannelUpdateName(VoiceChannelUpdateNameEvent event) {
        super.onVoiceChannelUpdateName(event);
    }

    @Override
    public void onVoiceChannelUpdatePosition(VoiceChannelUpdatePositionEvent event) {
        super.onVoiceChannelUpdatePosition(event);
    }

    @Override
    public void onVoiceChannelUpdateUserLimit(VoiceChannelUpdateUserLimitEvent event) {
        super.onVoiceChannelUpdateUserLimit(event);
    }

    @Override
    public void onVoiceChannelUpdateBitrate(VoiceChannelUpdateBitrateEvent event) {
        super.onVoiceChannelUpdateBitrate(event);
    }

    @Override
    public void onVoiceChannelUpdatePermissions(VoiceChannelUpdatePermissionsEvent event) {
        super.onVoiceChannelUpdatePermissions(event);
    }

    @Override
    public void onVoiceChannelUpdateParent(VoiceChannelUpdateParentEvent event) {
        super.onVoiceChannelUpdateParent(event);
    }

    @Override
    public void onVoiceChannelCreate(VoiceChannelCreateEvent event) {
        super.onVoiceChannelCreate(event);
    }

    @Override
    public void onRoleCreate(RoleCreateEvent event) {
        super.onRoleCreate(event);
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        super.onRoleDelete(event);
    }
}
