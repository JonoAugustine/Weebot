package com.ampro.main.entities;

import com.ampro.main.listener.events.BetterMessageEvent;

/**
 * An interface defining a Passive entity, which accepts events without direct
 * invocation.
 */
public interface Passive {

    /**
     * Take in a {@link BetterMessageEvent} to interact with.
     * @param event The event to receive.
     */
    void accept(BetterMessageEvent event);
}
