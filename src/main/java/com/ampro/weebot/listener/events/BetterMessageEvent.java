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

package com.ampro.weebot.listener.events;

import com.ampro.weebot.Launcher;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.events.message.priv.GenericPrivateMessageEvent;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;
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

    public enum TYPE {RECIVED, EDITED, DELETED}
    /** The original event */
    private final GenericMessageEvent messageEvent;
    private final Message message;
    private final TYPE type;
    /** The author of the event */
    private final User author;
    /** The Author as a Memeber */
    private final Member memberAuthor;
    /** Arguments of a MessageReceivedEvent */
    private final String[] arguments;

    /**
     * Construct a {@code BetterMessageEvent} from a
     * {@code MessageReceivedEvent}
     * @param event {@code GenericMessageEvent}
     *          to wrap.
     */
    public BetterMessageEvent(GenericMessageEvent event)
            throws InvalidEventException {
        super(event);

        this.message = event.getChannel().getMessageById(event.getMessageId())
                            .complete();
        //Locate the message in the channel
        this.author = this.message.getAuthor();
        this.memberAuthor = message.getMember();
        this.messageEvent = event;
        if (this.author == Launcher.getJda().getSelfUser()) {
            throw new InvalidAuthorException("User is self.");
        }

        this.arguments = message.getContentStripped().trim().split("\\s+", -1);

        if (event instanceof MessageDeleteEvent) {
            this.type = TYPE.DELETED;
        } else if (event instanceof MessageReceivedEvent) {
            this.type = TYPE.RECIVED;
        } else if (event instanceof MessageUpdateEvent) {
            this.type = TYPE.EDITED;
        } else {
            throw new InvalidEventException("Must be Update, Received, or Delete event");
        }
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
            throws InvalidAuthorException, InvalidEventException {
        super(event);
        this.message = event.getChannel().getMessageById(event.getMessageId())
                            .complete();

        if (author == Launcher.getJda().getSelfUser()) {
            throw new InvalidAuthorException("User is self.");
        } else {
            this.author = author;
            this.memberAuthor = message.getMember();
        }

        this.messageEvent = event;

        this.arguments = message.getContentStripped().trim().split("\\s+", -1);

        if (event instanceof MessageDeleteEvent) {
            this.type = TYPE.DELETED;
        } else if (event instanceof MessageReceivedEvent) {
            this.type = TYPE.RECIVED;
        } else if (event instanceof MessageUpdateEvent) {
            this.type = TYPE.EDITED;
        } else {
            throw new InvalidEventException("Must be Update, Received, or Delete event");
        }

    }

    /**
     * Send a message to the channel the event came from.
     * @param message String to send
     */
    public void reply(String message) {
        switch (this.messageEvent.getChannelType()) {
            case TEXT:
                this.messageEvent.getTextChannel().sendMessage(message).queue();
                break;
            case PRIVATE:
                this.author.openPrivateChannel().queue( (channel) ->
                        channel.sendMessage(message).queue()
                );
                break;
            default:
                System.err.println("Could not locate event channel.");
                break;
        }
    }

    /**
     * Send a message to the channel the event came from.
     * @param message String to send
     * @param consumer
     */
    public void reply(String message, Consumer<Message> consumer) {
        switch (this.messageEvent.getChannelType()) {
            case TEXT:
                this.messageEvent.getTextChannel().sendMessage(message)
                        .queue(m -> consumer.accept(m));
                break;
            case PRIVATE:
                this.author.openPrivateChannel().queue( (channel) ->
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
     * Reply with a file and message.
     * @param file
     * @param filename
     */
    public void reply(File file, String filename) {
        switch (this.messageEvent.getChannelType()) {
            case TEXT:
                this.messageEvent.getTextChannel().sendFile(file, filename).queue();
                break;
            case PRIVATE:
                this.author.openPrivateChannel().queue(
                        c -> c.sendFile(file, filename).queue()
                );
                break;
            default:
                System.err.println("Could not locate event channel.");
                break;
        }
    }

    public void reply(File file, String name, Consumer<File> consumer) {
        switch (this.messageEvent.getChannelType()) {
            case TEXT:
                this.messageEvent.getTextChannel().sendFile(file, name).queue();
                break;
            case PRIVATE:
                this.author.openPrivateChannel().queue(
                        c -> c.sendFile(file, name).queue()
                );
                break;
            default:
                System.err.println("Could not locate event channel.");
                return;
        }
        synchronized (file) { consumer.accept(file); }
    }

    public void reply(MessageEmbed embed) {
        this.message.getChannel().sendMessage(embed).queue();
    }

    /**
     * Send a private message to the author of the event.
     * @param message String to send
     */
    public void privateReply(String message) {
        switch (this.messageEvent.getChannelType()) {
            case TEXT:
                this.author.openPrivateChannel().queue((channel) ->
                    channel.sendMessage(message).queue()
                );
                return;
            default:
                this.reply(message);
                break;
        }
    }

    /**
     * Send a private message to the author of the event.
     * @param message Message string.
     * @param consumer Consumer lambda
     */
    public void privateReply(String message, Consumer<Message> consumer) {
        switch (this.messageEvent.getChannelType()) {
            case TEXT:
                this.author.openPrivateChannel().queue(
                        (channel) -> channel.sendMessage(message).queue(
                                m -> consumer.accept(m)
                        )
                );
                return;
            default:
                this.reply(message, consumer);
                break;
        }
    }

    /**
     * Send a file in a private channel.
     * @param file The file to send
     * @param filename The name of the file.
     */
    public void privateReply(File file, String filename) {
        switch (this.messageEvent.getChannelType()) {
            case TEXT:
                this.author.openPrivateChannel().queue(
                        (channel) -> channel.sendFile(file, filename).queue()
                );
                return;
            default:
                this.reply(file, filename);
                break;
        }
    }

    /**
     * Send a file in a private channel.
     * @param file
     * @param filename
     * @param consumer
     */
    public void privateReply(File file, String filename, Consumer<File> consumer) {
        switch (this.messageEvent.getChannelType()) {
            case TEXT:
                this.author.openPrivateChannel().queue(
                        (channel) -> channel.sendFile(file, filename).queue()
                );
                return;
            default:
                this.reply(file, filename, consumer);
                break;
        }
        synchronized (file) { consumer.accept(file); }
    }


    public void privateReply(MessageEmbed embed) {
        this.author.openPrivateChannel().queue( c -> {
            c.sendMessage(embed).queue();
        });
    }

    /** Delete the message. */
    public void deleteMessage() {
        this.messageEvent.getChannel().getMessageById(this.messageEvent.getMessageIdLong())
                .queue(m -> m.delete().queue());
    }

    /**
     * Get the arguments of a GenericMessageReceivedEvent.
     * @return String array of arguments.
     *          null if event is not MessageReceivedEvent
     */
    public String[] getArgs() {
        return this.arguments;
    }

    /**
     * An unmodifiable list of {@link net.dv8tion.jda.core.entities.Message.Attachment Attachments} that are attached to this message.
     * <br>Most likely this will only ever be 1 {@link net.dv8tion.jda.core.entities.Message.Attachment Attachment} at most.
     *
     * @return Unmodifiable list of {@link net.dv8tion.jda.core.entities.Message.Attachment Attachments}.
     */
    public List<Message.Attachment> getAttatchment() {
        return this.message.getAttachments();
    }

    @Override
    public Event getEvent() {
        return this.messageEvent;
    }

    @Override
    public User getAuthor() {
        return this.author;
    }

    @Override
    public boolean isPrivate() {
        return this.messageEvent.isFromType(ChannelType.PRIVATE);
    }

    /**
     * @return The channel of origin.
     */
    public MessageChannel getMessageChannel() {
        return this.messageEvent.getChannel();
    }

    public Guild getGuild() {
        return this.messageEvent.getGuild();
    }

    /**
     * @return
     *          The TextChannel the Message was received in
     *          or null if not from a TextChannel
     */
    public TextChannel getTextChannel() {
        return this.messageEvent.getTextChannel();
    }

    /**
     * @return {@link OffsetDateTime} of the message.
     */
    public final OffsetDateTime getCreationTime() {
        return this.message.getCreationTime();
    }

    /** @return The {@link Message} wrapped. */
    public final Message getMessage() {
        return message;
    }

    /** @return The Author as a {@link net.dv8tion.jda.core.entities.Member}.
     *          Null if not from a guild.
     */
    public final Member getMember() {
        return memberAuthor;
    }

    public final TYPE getType() {
        return this.type;
    }

    /** @return {@link Message#getContentDisplay()} */
    @Override
    public String toString() { return this.message.getContentDisplay(); }

}
