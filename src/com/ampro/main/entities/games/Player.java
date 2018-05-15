/**
 *
 */

package com.ampro.main.entities.games;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;

import java.util.function.Consumer;

/**
 * Base Wrapper Class for Members currently involved in a Webot {@code Game}
 */
public abstract class Player {

    /** User this Player is wrapped around */
    private final User user;

    /**
     * Make a new player wrapper for a User.
     * @param user {@code net.dv8tion.jda.core.entities.User}
     */
    protected Player(User user) {
        this.user = user;
    }

    /**
     * @return {@code net.dv8tion.jda.core.entities.User}
     *                  this player is wrapped around
     */
    public User getUser() {
        return this.user;
    }

    /**
     * Send a private message to the {@code Player}.
     * @param message String to send
     */
    private void privateMessage(String message) {
        this.user.openPrivateChannel().complete()
                .sendMessage(message).queue();
    }

    /**
     * Send a private message to the {@code Player}.
     * @param message Message to send
     */
    public void privateMessage(Message message) {
        this.privateMessage(message.getContentRaw());
    }

    public void privateMessage(String message, Consumer<Message> consumer) {
        this.user.openPrivateChannel().queue( privateChannel -> {
           privateChannel.sendMessage(message).queue( m -> {
               consumer.accept(m);
           });
        });
    }

}
