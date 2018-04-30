package com.ampro.main.listener;

import net.dv8tion.jda.core.events.message.priv.PrivateMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageEmbedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageUpdateEvent;
import net.dv8tion.jda.core.events.message.priv.react.PrivateMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.priv.react.PrivateMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * AThe redistribution center for all incoming Private events.
 *
 * @author Jonathan Augustine
 */
public class PrivateListener extends ListenerAdapter {

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
    }

    @Override
    public void onPrivateMessageUpdate(PrivateMessageUpdateEvent event) {
    }

    @Override
    public void onPrivateMessageDelete(PrivateMessageDeleteEvent event) {
    }

    @Override
    public void onPrivateMessageEmbed(PrivateMessageEmbedEvent event) {
    }

    @Override
    public void onPrivateMessageReactionAdd(PrivateMessageReactionAddEvent event) {
    }

    @Override
    public void onPrivateMessageReactionRemove(PrivateMessageReactionRemoveEvent event) {
    }


}
