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

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * An wrapper class for {@code net.dv8tion.events.message.GenereicMessageEvent}
 * that allows for easier
 * responding to messages in a number of ways; <br> namely, this class contains
 * methods for replying to messages regardless of the channel of origin, <br>
 * responding to events directly to a {@code net.dv8tion.entites.PrivateChannel}
 *
 * @author Jonathan Augustine
 */
public class BetterMessageEvent {

    /** The original event */
    private final GenericMessageEvent EVENT;
    /** The author (User) of the event */
    private final User AUTHOR;
    private final String[] ARGUMENTS;


    /**
     * Construct a {@code BetterMessageEvent} from a
     * {@code net.dv8tion.jda.core.events.message.MessageReceivedEvent}
     * @param event
     */
    public BetterMessageEvent(GenericMessageEvent event) {
        this.EVENT = event;
        //Locate the message in the channel
        this.AUTHOR = event.getChannel().getMessageById(event.getMessageId())
                            .complete() //Use complete to get the return value
                            .getAuthor(); //Get the author (User)
        if (event instanceof MessageReceivedEvent)
            this.ARGUMENTS = ((MessageReceivedEvent) event)
                    .getMessage().getContentStripped().split(" ");
         else
            this.ARGUMENTS = null;
    }

    /**
     * Send a message to the channel the event came from.
     * @param message String to send
     */
    public void reply(String message) {
        switch (this.EVENT.getChannelType()) {
            case TEXT:
                this.EVENT.getTextChannel().sendMessage(message).queue();
                return;
            case PRIVATE:
                this.EVENT.getPrivateChannel().sendMessage(message).queue();
                return;
            default:
                System.err.println("Could not locate event channel.");
        }
    }

    /**
     * Send a message to the channel the event came from.
     * @param message {@code net.dv8tion.jda.core.entities.Message} to send
     */
    public void reply(Message message) {
        this.reply(message.getContentRaw());
    }

    /**
     * Send a private message to the author of the event.
     * @param message String to send
     */
    public void privateReply(String message) {
        switch (this.EVENT.getChannelType()) {
            case TEXT:
                this.AUTHOR.openPrivateChannel()
                        .complete()
                        .sendMessage(message);
                return;
            default:
                this.reply(message);
                return;
        }
    }

    /**
     * Send a private message to the author of the event.
     * @param {@code net.dv8tion.jda.core.entities.Message} to send
     */
    public void privateReply(Message message) {
        switch (this.EVENT.getChannelType()) {
            case TEXT:
                this.AUTHOR.openPrivateChannel()
                        .complete()
                        .sendMessage(message);
                return;
            default:
                this.reply(message);
                return;
        }
    }


}
