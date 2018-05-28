package com.ampro.weebot.entities.bot;

import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.listener.events.BetterEvent;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.lang.management.LockInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalWeebot extends Weebot implements EventListener {

    /** A Map of {@link IPassive} objects mapped to user IDs */
    private final ConcurrentHashMap<Long, List<IPassive>> USER_PASSIVES
            = new ConcurrentHashMap<>();

    /**
     * Create a Weebot with no guild or call sign.
     * <b>This is only called for the private-chat instance Weebot created
     * when a new Database is created.</b>
     */
    public GlobalWeebot() { super(); }

    /**
     * Distribute each event to all the Global Weebot's {@link IPassive} objects.
     * @param event  The Event to handle.
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

    /** @return */
    public  final synchronized Map<Long, List<IPassive>> getUserPassives() {
        return USER_PASSIVES;
    }

    /**
     * @param user The user
     * @return The List of {@link com.ampro.weebot.commands.IPassive IPassives} mapped
     * to the given user. Null if there are no passives mapped to the user.
     */
    public  final synchronized List<IPassive> getUserPassives(User user) {
        return USER_PASSIVES.get(user.getIdLong());
    }

    /**
     * Add an {@link IPassive} to the the list mapped to the {@link User} given.<br>
     * If there is no mapping to the {@link User user} or it is mapped to {@code null},
     * a new {@link List} is created, mapped to the {@link User user} and the
     * {@link IPassive} is added.
     *
     * @param user The user
     * @param passive The passive to map to the User
     * @return {@code false} if the {@link IPassive} already exists in the
     * {@link User user's} list of {@link IPassive passives}.
     */
    public final synchronized boolean addUserPassive(User user, IPassive passive) {
        List<IPassive> pasList = USER_PASSIVES.get(user);
        if (pasList == null) {
            pasList = new ArrayList<>();
            USER_PASSIVES.put(user.getIdLong(), pasList);
        }
        return pasList.add(passive);
    }

    /**
     * @param passive
     * @return {@code true} if the user's passive list contains the parameter.<br>
     *     {@code false} if the user's passive list does not contain the parameter or
     *     is null
     */
    public final synchronized boolean userHasPassive(User user, IPassive passive) {
        try {
            return USER_PASSIVES.get(user.getIdLong()).contains(passive);
        } catch (NullPointerException e) {
            return false;
        }
    }

}

