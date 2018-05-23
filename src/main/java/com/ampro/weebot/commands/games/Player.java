/**
 *
 */

package com.ampro.weebot.commands.games;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;

import java.util.function.Consumer;

/**
 * Base Wrapper Class for Members currently involved in a Webot {@code Game}
 */
public abstract class Player implements Comparable<Player> {

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
        this.user.openPrivateChannel().queue(
                channel -> channel.sendMessage(message).queue()
        );
    }

    /**
     * Send a private message to the {@link Player}.
     * @param message The message.
     * @param consumer Lambda
     */
    public void privateMessage(String message, Consumer<Message> consumer) {
        this.user.openPrivateChannel().queue( privateChannel -> {
           privateChannel.sendMessage(message).queue( m -> {
               consumer.accept(m);
           });
        });
    }

    /**
     * Send a private embed message to the player.
     * @param embed The MessageEmbed to send.
     * @param consumer Message Consumer
     */
    public void privateMessage(MessageEmbed embed, Consumer<Message> consumer) {
        this.user.openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessage(embed).queue( message -> consumer.accept(message));
        });
    }

    @Override
    public int compareTo(Player p2) {
        return (int) (this.user.getIdLong() - p2.user.getIdLong());
    }

}
