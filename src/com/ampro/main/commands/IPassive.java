package com.ampro.main.commands;

import com.ampro.main.listener.events.BetterMessageEvent;

/**
 * An interface defining a IPassive entity, which accepts events without direct
 * invocation.
 */
public interface IPassive {

    /**
     * Take in a {@link BetterMessageEvent} to interact with.
     * @param event The event to receive.
     */
    void accept(BetterMessageEvent event);
}
