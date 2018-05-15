package com.ampro.main.game;


import com.ampro.main.bot.Weebot;
import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;

/**
 * Players must slip phrases into sentences without being found out.
 * TODO: How to accept messages (event listener? vs from bot?)
 * @author Jonathan Augustine, Daniel Ernst
 */
public class SecretePhrase extends Game<SecretePhrase.SPPlayer> {

    public static final class SPPlayer extends Player {

        private final ArrayList<String> unusedPhrases;
        private final ArrayList<String> usedPhrases;

        /**
         * Make a new player wrapper for a User.
         *
         * @param user
         *         {@code net.dv8tion.jda.core.entities.User}
         */
        protected SPPlayer(User user) {
            super(user);
            this.unusedPhrases = new ArrayList<>();
            this.usedPhrases   = new ArrayList<>();
        }

    }


    public SecretePhrase(Weebot bot, User author) {
        super(bot, author);
    }

    public SecretePhrase(Weebot bot, User author, User...players) {
        super(bot, author);
    }

    @Override
    protected int startGame() {
        return 0;
    }

    @Override
    protected int endGame() {
        return 0;
    }

    //Member Make a game

    //Players join

    //Author starts game

    //Game
        //player says phrase
        //If other player calls out phrase exactly, point removed (or added?)

    //Time Runs out or Author ends game

    /**
     * Generate "random" (?) phrases to give to a {@link SPPlayer player} in a private
     * channel.
     * @param phrases The number of phrases to generate.
     * @return An {@link String string} array of the param size filled with phrases.
     */
    private final String[] generatePhrases(int phrases) {
        String[] out = new String[phrases];
        //TODO How to generate phrases?
        return out;
    }
}
