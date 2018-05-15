package com.ampro.main.entities.games;


import com.ampro.main.entities.Passive;
import com.ampro.main.entities.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Players must slip phrases into sentences without being found out.
 * TODO: How to accept messages (event listener? vs from bot?)
 * TODO: Should SecretePhrase and SecretePhraseCommand be in one file?
 * @author Jonathan Augustine, Daniel Ernst
 */
public class SecretePhrase extends Game<SecretePhrase.SPPlayer> implements Passive {

    private static final class Phrase {
        private final String phrase;
        private final OffsetDateTime creationTime;
        private OffsetDateTime useTime;

        Phrase(String phrase) {
            this.phrase = phrase;
            this.creationTime = OffsetDateTime.now();
        }
    }

    static final class SPPlayer extends Player {
        private final ArrayList<Phrase> unusedPhrases;
        private final ArrayList<Phrase> usedPhrases;
        private final ArrayList<Phrase> caughtPhrases;

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
            this.caughtPhrases = new ArrayList<>();
        }

    }

    public SecretePhrase(Weebot bot, User author) {
        super(bot, author);
    }

    public SecretePhrase(Weebot bot, User author, User...players) {
        super(bot, author);
    }

    @Override
    public void accept(BetterMessageEvent event) {
        Member member = event.getMember();
        SPPlayer memberPlayer = this.PLAYERS.get(member.getUser().getIdLong());
        if (memberPlayer == null) return;
        String message = event.getMessage().getContentStripped();
        for (Phrase p : memberPlayer.unusedPhrases) {
            if (message.toLowerCase().contains(p.phrase.toLowerCase())) {
                memberPlayer.unusedPhrases.remove(p);
                memberPlayer.usedPhrases.add(p);
                return;
            }
        }
    }

    /**
     * Adds {@code player} to games and generates phrases for them.
     *
     * @param player Player to add
     * @return -1 if player could not be added
     *          0 if player is already in Game
     *          1 if player was added
     */
    @Override
    protected int joinGame(SPPlayer player) {
        switch(super.joinGame(player)) {
            case -1:
                return -1;
            case 0:
                return 0;
            default:
                player.unusedPhrases.addAll(
                        Arrays.asList(this.generatePhrases(0)));
                return 1;
        }
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
    private final Phrase[] generatePhrases(int phrases) {
        Phrase[] out = new Phrase[phrases];
        //TODO How to generate phrases?
        return out;
    }
}
