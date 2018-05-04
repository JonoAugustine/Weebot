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
import com.ampro.main.listener.events.BetterEvent;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
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
                    //Wrap the event in a BetterMessageEvent and send to bot
                    .readEvent(new BetterMessageEvent(event));
        } catch (ClassCastException e) {
            System.err.println("Failed to cast to Weebot");
            e.printStackTrace();
        } catch (BetterEvent.InvalidEventException e) {
            System.err.println("Construction of BetterEvent using " +
                    "Invalid event attempted");
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Launcher.getDatabase().addBot(new Weebot(event.getGuild()));
        event.getGuild().getDefaultChannel().sendMessage(
                "Welcome, me! \n(call me with ``<>``)"
        ).queue();
    }

}
