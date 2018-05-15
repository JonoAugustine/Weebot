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
import net.dv8tion.jda.core.events.message.priv.GenericPrivateMessageEvent;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

/**
 * An wrapper class for {@link GenericMessageEvent}
 * & {@link GenericPrivateMessageEvent}
 * that allows for easier
 * responding to messages in a number of ways; namely, this class contains
 * methods for replying to messages regardless of the channel of origin, <br>
 * responding to events directly to a {@link PrivateChannel}.
 *
 * @author Jonathan Augustine
 */
public class BetterMessageEvent extends BetterEvent {

    /** The original event */
    private final GenericMessageEvent MESSAGE_EVENT;
    private final Message MESSAGE;
    /** The author of the event */
    private final User AUTHOR;
    /** The Author as a Memeber */
    private final Member M_AUTHOR;
    /** Arguments of a MessageReceivedEvent */
    private final String[] ARGUMENTS;
    private OffsetDateTime CREATION_TIME;

    /**
     * Construct a {@code BetterMessageEvent} from a
     * {@code MessageReceivedEvent}
     * @param event {@code GenericMessageEvent}
     *          to wrap.
     */
    public BetterMessageEvent(GenericMessageEvent event)
            throws InvalidEventException {
        super(event);

        this.MESSAGE = event.getChannel().getMessageById(event.getMessageId())
                            .complete();
        //Locate the MESSAGE in the channel
        this.AUTHOR = this.MESSAGE.getAuthor();
        this.M_AUTHOR = MESSAGE.getMember();
        this.MESSAGE_EVENT = event;
        if (this.AUTHOR == Launcher.getJda().getSelfUser()) {
            throw new InvalidAuthorException("User is self.");
        }

        this.ARGUMENTS = MESSAGE.getContentStripped().trim().split(" ");
        this.CREATION_TIME = MESSAGE.getCreationTime();
    }

    /**
     * Construct a {@code BetterMessageEvent} from a
     * {@code MessageReceivedEvent}
     * @param event {@code GenericMessageEvent}
     *          to wrap.
     * @param author {@link net.dv8tion.jda.core.entities.User}
     *                 who sent the MESSAGE.
     */
    public BetterMessageEvent(GenericMessageEvent event, User author)
            throws InvalidAuthorException {
        super(event);
        this.MESSAGE = event.getChannel().getMessageById(event.getMessageId())
                            .complete();

        if (author == Launcher.getJda().getSelfUser()) {
            throw new InvalidAuthorException("User is self.");
        } else {
            this.AUTHOR = author;
            this.M_AUTHOR = MESSAGE.getMember();
        }

        this.MESSAGE_EVENT = event;

        this.ARGUMENTS = MESSAGE.getContentStripped().trim().split(" ");
        this.CREATION_TIME = MESSAGE.getCreationTime();


    }

    /**
     * Send a MESSAGE to the channel the event came from.
     * @param message String to send
     */
    public void reply(String message) {
        switch (this.MESSAGE_EVENT.getChannelType()) {
            case TEXT:
                this.MESSAGE_EVENT.getTextChannel().sendMessage(message).queue();
                break;
            case PRIVATE:
                this.AUTHOR.openPrivateChannel().queue( (channel) ->
                        channel.sendMessage(message).queue()
                );
                break;
            default:
                System.err.println("Could not locate event channel.");
                break;
        }
    }

    /**
     * Send a MESSAGE to the channel the event came from.
     * @param message String to send
     * @param consumer
     */
    public void reply(String message, Consumer<Message> consumer) {
        switch (this.MESSAGE_EVENT.getChannelType()) {
            case TEXT:
                this.MESSAGE_EVENT.getTextChannel().sendMessage(message)
                        .queue(m -> consumer.accept(m));
                break;
            case PRIVATE:
                this.AUTHOR.openPrivateChannel().queue( (channel) ->
                    channel.sendMessage(message).queue( m ->
                            consumer.accept(m)
                    )
                );
                break;
            default:
                System.err.println("Could not locate event channel.");
                break;
        }

    }


    /**
     * Send a MESSAGE to the channel the event came from.
     * @param message {@code net.dv8tion.jda.core.entities.Message} to send
     */
    private void reply(Message message) {
        this.reply(message.getContentRaw());
    }

    /**
     * Reply with a file and MESSAGE.
     * @param file
     * @param filename
     */
    public void reply(File file, String filename) {
        switch (this.MESSAGE_EVENT.getChannelType()) {
            case TEXT:
                this.MESSAGE_EVENT.getTextChannel().sendFile(file, filename).queue();
                break;
            case PRIVATE:
                this.AUTHOR.openPrivateChannel().queue(
                        c -> c.sendFile(file, filename).queue()
                );
                break;
            default:
                System.err.println("Could not locate event channel.");
                break;
        }
    }

    public void reply(File file, String name, Consumer<File> consumer) {
        switch (this.MESSAGE_EVENT.getChannelType()) {
            case TEXT:
                this.MESSAGE_EVENT.getTextChannel().sendFile(file, name).queue();
                break;
            case PRIVATE:
                this.AUTHOR.openPrivateChannel().queue(
                        c -> c.sendFile(file, name).queue()
                );
                break;
            default:
                System.err.println("Could not locate event channel.");
                return;
        }
        synchronized (file) { consumer.accept(file); }
    }

    /**
     * Send a private MESSAGE to the author of the event.
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
                break;
        }
    }

    /**
     * Send a private MESSAGE to the author of the event.
     * @param message {@link Message} to send
     */
    public void privateReply(Message message) {
        switch (this.MESSAGE_EVENT.getChannelType()) {
            case TEXT:
                this.AUTHOR.openPrivateChannel().queue( c ->
                    c.sendMessage(message).queue()
                );
                return;
            default:
                this.reply(message);
                break;
        }
    }

    /** Delete the MESSAGE. */
    public void deleteMessage() {
        this.MESSAGE_EVENT.getChannel().getMessageById(this.MESSAGE_EVENT.getMessageIdLong())
                .queue(m -> m.delete().queue());
    }

    /**
     * Get the arguments of a GenericMessageReceivedEvent.
     * @return String array of arguments.
     *          null if event is not MessageReceivedEvent
     */
    public String[] getArgs() {
        return this.ARGUMENTS;
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
    public MessageChannel getMessageChannel() {
        return this.MESSAGE_EVENT.getChannel();
    }

    public Guild getGuild() {
        return this.MESSAGE_EVENT.getGuild();
    }

    /**
     * @return
     *          The TextChannel the Message was received in
     *          or null if not from a TextChannel
     */
    public TextChannel getTextChannel() {
        return this.MESSAGE_EVENT.getTextChannel();
    }

    /**
     * @return {@link OffsetDateTime} of the MESSAGE.
     */
    public final OffsetDateTime getCreationTime() {
        return this.CREATION_TIME;
    }

    /** @return The {@link Message} wrapped. */
    public final Message getMessage() {
        return MESSAGE;
    }

    /** @return The Author as a {@link net.dv8tion.jda.core.entities.Member}.
     *          Null if not from a guild.
     */
    public final Member getMember() {
        return M_AUTHOR;
    }

}
