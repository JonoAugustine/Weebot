/**
 * 
 */

package com.ampro.main.bot.game;

import net.dv8tion.jda.core.entities.User;

/**
 * Base Wrapper Class for Members currently involved in a Webot {@code Game}
 */
public abstract class Player {
    
    //Who am I?
    private final User user;
    
    /**
     * Make a new player wrapper for a User
     */
    public Player(User user) {
        this.user = user;
    }

    /**
     * @return User
     */
    protected User getUser() {
        return this.user;
    }
    
}