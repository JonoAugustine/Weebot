package com.ampro.weebot.commands.properties;

import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains validation information and methods to restrict access to any entity.
 * <br>
 *     Restrictions by positivity (disallow or allow access). <br>
 *     Restrictions can be made by {@link Role}, {@link TextChannel}, or
 *     {@link User}. <br>
 *     Restrictions are made using
 *     {@link net.dv8tion.jda.core.entities.ISnowflake} Long IDs. <br>
 *
 *
 * @author Jonathan Augustine
 *
 */
public class Restriction {

    /** An indication of the Command's restriction in a guild. */
    public enum Status {
        /** The Command has no restrictions */
        OPEN,
        /** The Command has restrictions */
        RESTRICTED,
        /** The Command is disabled */
        DISABLED
    }

    private final List<Long> allowedUsers;
    private final List<Long> blockedUsers;
    private final List<Long> allowedRoles;
    private final List<Long> blockedRoles;
    private final List<Long> allowedTextChannels;
    private final List<Long> blockedTextChannels;

    /** Initialize all restriction arrays */
    public Restriction() {
        this.allowedUsers = new ArrayList<>();
        this.blockedUsers = new ArrayList<>();
        this.allowedRoles = new ArrayList<>();
        this.blockedRoles = new ArrayList<>();
        this.allowedTextChannels = new ArrayList<>();
        this.blockedTextChannels = new ArrayList<>();
    }

    /** @return An {@link Collections#unmodifiableList(List)} of allowed users. */
    public List<Long> getAllowedUsers() {
        return Collections.unmodifiableList(allowedUsers);
    }

    /** @return An {@link Collections#unmodifiableList(List)} of blocked users. */
    public List<Long> getBlockedUsers() {
        return Collections.unmodifiableList(blockedUsers);
    }

    /**
     * Add an allowed {@link User}.
     * Will remove the user from the "blocked" list if present.
     * @param user The user to allow
     */
    public void allow(User user) {
        this.blockedUsers.remove(user.getIdLong());
        this.allowedUsers.add(user.getIdLong());
    }

    /**
     * Block a user's access.
     * Will remove the user from "allowed" list if present
     * @param user The user to black access from
     */
    public void block(User user) {
        this.allowedUsers.remove(user.getIdLong());
        this.blockedUsers.add(user.getIdLong());
    }

    /**
     * Check if a user has been allowed or blocked.
     * @param user The user to check
     * @return {@code true} if the user is allowed, or there are no restrictions.
     */
    public boolean isAllowed(User user) {
        if (!this.allowedUsers.isEmpty()) {
            return this.allowedUsers.contains(user.getIdLong());
        } else if (!this.blockedUsers.isEmpty()) {
            return !this.blockedUsers.contains(user.getIdLong());
        } else {
            return true;
        }
    }

    /** @return An {@link Collections#unmodifiableList(List)} of allowed Roles. */
    public List<Long> getAllowedRoles() {
        return Collections.unmodifiableList(allowedRoles);
    }

    /** @return An {@link Collections#unmodifiableList(List)} of blocked Roles. */
    public List<Long> getBlockedRoles() {
        return Collections.unmodifiableList(blockedRoles);
    }

    /**
     * Allow a Role access.
     * Will remove the role from the "blocked" list if present.
     * @param role The role to allow access
     */
    public void allow(Role role) {
        this.blockedRoles.remove(role.getIdLong());
        this.allowedRoles.add(role.getIdLong());
    }

    /**
     * Block a Role's access.
     * Will remove the Role from the "blocked" list if present.
     * @param role The role to block access.
     */
    public void block(Role role) {
        this.allowedRoles.remove(role.getIdLong());
        this.blockedRoles.add(role.getIdLong());
    }

    /**
     * Check if a Role has been allowed or blocked.
     * @param role The user to check
     * @return {@code true} if the role is allowed, or there are no restrictions.
     */
    public boolean isAllowed(Role role) {
        if (!this.allowedRoles.isEmpty()) {
            return this.allowedRoles.contains(role.getIdLong());
        } else if (!this.blockedRoles.isEmpty()) {
            return !this.blockedRoles.contains(role.getIdLong());
        } else {
            return true;
        }
    }

    /** @return An {@link Collections#unmodifiableList(List)} of allowed TextChannels. */
    public List<Long> getAllowedTextChannels() {
        return Collections.unmodifiableList(allowedTextChannels);
    }

    /** @return An {@link Collections#unmodifiableList(List)} of blocked TextChannels. */
    public List<Long> getBlockedTextChannels() {
        return Collections.unmodifiableList(blockedTextChannels);
    }

    /**
     * Allow a TextChannel access.
     * Will remove the channel from the "blocked" list if present.
     * @param channel The TextChannel to allow access
     */
    public void allow(TextChannel channel) {
        this.blockedTextChannels.remove(channel.getIdLong());
        this.allowedTextChannels.add(channel.getIdLong());
    }

    /**
     * Block a TextChannel access.
     * Will remove the channel from the "allowed" list if present.
     * @param channel The TextChannel to block access
     */
    public void block(TextChannel channel) {
        this.allowedTextChannels.remove(channel.getIdLong());
        this.blockedTextChannels.add(channel.getIdLong());
    }

    /**
     * Check if a TextChannel has been allowed or blocked.
     * @param channel The TextChannel to check
     * @return {@code true} if the TextChannel is allowed,
     *          or there are no restrictions.
     */
    public boolean isAllowed(TextChannel channel) {
        if (!this.allowedTextChannels.isEmpty()) {
            return this.allowedTextChannels.contains(channel.getIdLong());
        } else if (!this.blockedTextChannels.isEmpty()) {
            return !this.blockedTextChannels.contains(channel.getIdLong());
        } else {
            return true;
        }
    }

    /** @return {@code true} if there are any restrictions.*/
    public boolean restricted() {
        return allowedUsers.isEmpty() || blockedUsers.isEmpty()
                || allowedRoles.isEmpty() || blockedRoles.isEmpty()
                || allowedTextChannels.isEmpty() || blockedTextChannels .isEmpty();
    }

}
