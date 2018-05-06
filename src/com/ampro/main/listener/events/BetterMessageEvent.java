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

package com.ampro.main.listener.events;

import com.ampro.main.Launcher;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;

/**
 * An wrapper class for {@code GenereicMessageEvent} & {@code GenericPrivateMessageEvent}
 * that allows for easier
 * responding to messages in a number of ways; <br> namely, this class contains
 * methods for replying to messages regardless of the channel of origin, <br>
 * responding to events directly to a {@code net.dv8tion.entites.PrivateChannel}.
 *
 * @author Jonathan Augustine
 */
public class BetterMessageEvent extends BetterEvent {

    /** The original event */
    private final GenericMessageEvent MESSAGE_EVENT;
    /** The author (User) of the event */
    private final User AUTHOR;
    /** Arguments of a MessageReceivedEvent */
    private String[] ARGUMENTS;

    /**
     * Construct a {@code BetterMessageEvent} from a
     * {@code MessageReceivedEvent}
     * @param event {@code GenericMessageEvent}
     *          to wrap.
     */
    public BetterMessageEvent(GenericMessageEvent event)
            throws InvalidAuthorException {
        super(event);
        //Locate the message in the channel
        this.AUTHOR = event.getChannel().getMessageById(event.getMessageId())
                            .complete() //Use complete to get the return value
                            .getAuthor(); //Get the author (User)
        this.MESSAGE_EVENT = event;
        if (this.AUTHOR == Launcher.getJda().getSelfUser()) {
            throw new InvalidAuthorException("User is self.");
        }
        event.getChannel().getMessageById(event.getMessageId())
                .queue(message ->
                    this.ARGUMENTS = message.getContentStripped().split(" ")
                );
    }

    /**
     * Construct a {@code BetterMessageEvent} from a
     * {@code MessageReceivedEvent}
     * @param event {@code GenericMessageEvent}
     *          to wrap.
     * @param author {@link net.dv8tion.jda.core.entities.User}
     *                 who sent the message.
     */
    public BetterMessageEvent(GenericMessageEvent event, User author)
            throws InvalidAuthorException {
        super(event);
        if (author == Launcher.getJda().getSelfUser()) {
            throw new InvalidAuthorException("User is self.");
        } else {
            this.AUTHOR = author;
        }
        this.MESSAGE_EVENT = event;
        event.getChannel().getMessageById(event.getMessageId())
                .queue(message ->
                    this.ARGUMENTS = message.getContentStripped().split(" ")
                );
    }

    /**
     * Send a message to the channel the event came from.
     * @param message String to send
     */
    public void reply(String message) {
        switch (this.MESSAGE_EVENT.getChannelType()) {
            case TEXT:
                this.MESSAGE_EVENT.getTextChannel().sendMessage(message).queue();
                return;
            case PRIVATE:
                this.AUTHOR.openPrivateChannel().queue( (channel) ->
                    channel.sendMessage(message).queue()
                );
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
        switch (this.MESSAGE_EVENT.getChannelType()) {
            case TEXT:
                this.AUTHOR.openPrivateChannel().queue((channel) ->
                    channel.sendMessage(message).queue()
                );
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
        switch (this.MESSAGE_EVENT.getChannelType()) {
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

    /** Delete the message. */
    public void deleteMessage() {
        this.MESSAGE_EVENT.getChannel().getMessageById(this.MESSAGE_EVENT.getMessageIdLong())
                .complete().delete().queue();
    }

    /**
     * Get the arguments of a GenericMessageReceivedEvent.
     * @return String array of arguments.
     *          null if event is not MessageReceivedEvent
     */
    public String[] getArgs() {
        return this.ARGUMENTS.clone();
    }

    @Override
    public Event getEvent() {
        return this.MESSAGE_EVENT;
    }

    @Override
    public User getAuthor() {
        return this.AUTHOR;
    }

    @Override
    public boolean isPrivate() {
        return this.MESSAGE_EVENT.isFromType(ChannelType.PRIVATE);
    }

    /**
     * @return The channel of origin.
     */
    public MessageChannel getChannel() {
        return this.MESSAGE_EVENT.getChannel();
    }

    public Guild getGuild() {
        return this.MESSAGE_EVENT.getGuild();
    }

    public User getSelfUser() {
        return this.getSelfUser();
    }

    public Member getSelfMember() {
        return this.getSelfMember();
    }

}
