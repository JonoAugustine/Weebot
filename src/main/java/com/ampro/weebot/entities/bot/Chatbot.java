package com.ampro.weebot.entities.bot;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import com.google.code.chatterbotapi.*;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Random;

/**
 * A passive chatbot enabled with {@link Weebot#ACTIVE_PARTICIPATE} using
 * <a href="https://github.com/pierredavidbelanger/chatter-bot-api">Chatterbot API</a>.
 * <br>TODO: All of it
 */
public class Chatbot implements IPassive {

    private final long guildID;

    private boolean dead;

    private transient ChatterBotFactory factory;

    private transient ChatterBot BOT;

    private transient ChatterBotSession SESH;

    /** The last message sent by the chatbot */
    private String lastThought;

    /** Is the bot in a conversation?*/
    private transient boolean talking;
    private transient int talkedFor;
    private final int TALKLIM = 30;

    @Override
    public void accept(Weebot bot, BetterMessageEvent event) {
        if (this.dead) return;
        if (bot.validateCallsign(event.getArgs()) > 0) return;
        if (!bot.canParticipate()) {
            this.dead = true;
            return;
        }
        if (factory == null) {
            this.factory = new ChatterBotFactory();
            try {
                BOT = factory.create(ChatterBotType.PANDORABOTS,"b0dafd24ee35a477");
            } catch (Exception e) { return; }
            SESH = BOT.createSession();
        }

        if (!talking) {

            int chance =
                    (event.mentions(Launcher.getJda().getSelfUser())
                            || event.toString().toLowerCase().contains("weebot")) ?
                    7 : 19;

            //IDK just a placeholder
            switch (new Random().nextInt() % chance) {
                case 0:
                    talking = true;
                    break;
                default:
                    return;
            }
        }

        try {
            SESH.think("Your name is Weebot.");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String rpl;
        try {
            rpl = SESH.think(event.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (rpl.equalsIgnoreCase(lastThought)) {
            event.reply("...I don't really wanna chat anymore...");
            SESH = BOT.createSession();
            talking = false;
            return;
        }

        if (rpl.isEmpty()) {
            event.reply("...I don't really wanna chat anymore...");
            SESH = BOT.createSession();
            talking = false;
            return;
        }

        event.reply(rpl);
        if(talkedFor++ >= TALKLIM) {
            event.reply("I'm feeling a little tired. I'll talk more later!");
            talkedFor = 0;
        }
        lastThought = rpl;

    }

    public Chatbot(Guild guild) throws Exception {
        this.guildID = guild.getIdLong();
        this.factory = new ChatterBotFactory();

        BOT = factory.create(ChatterBotType.PANDORABOTS,"b0dafd24ee35a477");
        SESH = BOT.createSession();

    }

    @Override
    public boolean dead() { return dead; }

}
