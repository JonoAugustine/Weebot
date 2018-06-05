package com.ampro.weebot.commands;

import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;

/**
 * An interface defining a IPassive entity, which accepts events without direct
 * invocation.
 *
 //TODO Introduce Passives into public Weebot data in help
 */
public interface IPassive {

    /**
     * Take in a {@link BetterMessageEvent} to interact with.
     * @param event The event to receive.
     */
    void accept(Weebot bot, BetterMessageEvent event);

    /** @return {@code false} if the passive is no longer active */
    boolean dead();

}
