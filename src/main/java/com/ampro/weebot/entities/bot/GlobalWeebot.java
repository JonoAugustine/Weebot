package com.ampro.weebot.entities.bot;

import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.listener.events.BetterEvent;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class GlobalWeebot extends Weebot implements EventListener {

    /**
     * Create a Weebot with no guild or call sign.
     * <b>This is only called for the private-chat instance Weebot created
     * when a new Database is created.</b>
     */
    public GlobalWeebot() {
        super();
    }

    /**
     * Handles any {@link Event Event}.
     *
     * <p>To get specific events with Methods like {@code onMessageReceived
     * (MessageReceivedEvent
     * event)}
     * take a look at: {@link ListenerAdapter ListenerAdapter}
     *
     * @param event
     *         The Event to handle.
     */
    @Override
    public void onEvent(Event event) {
        if (event instanceof MessageReceivedEvent) {
            if(((MessageReceivedEvent) event).getAuthor().isBot()) {
                return;
            }
            this.PASSIVES.forEach(p -> {
                try {
                    p.accept(new BetterMessageEvent((MessageReceivedEvent) event,
                                                    ((MessageReceivedEvent) event).getAuthor()
                    ));
                } catch (BetterEvent.InvalidEventException e) {
                    e.printStackTrace();
                    System.err.println("Err in GlobalWeebot BetterMessageEvent wrapper");
                }
            });
        }
    }

}
