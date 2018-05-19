package com.ampro.weebot.commands.games;


import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SecretePhraseCommand extends Command {

    /**
     * Players must slip phrases into sentences without being found out.
     * <br>
     *     Played on @everyone {@link TextChannel TextChannels}.
     * <br>
     * Messages are un-callable after {@link SecretePhrase#callMessageLimit}
     * number of player messages.
     *
     * @author Jonathan Augustine, Daniel Ernst
     */
    public static final class SecretePhrase extends Game<SecretePhrase.SPPlayer>
    implements IPassive {

        private static final class Phrase {

            private final String phrase;
            private final OffsetDateTime creationTime;
            private OffsetDateTime useTime;

            Phrase(String phrase) {
                this.phrase = phrase;
                this.creationTime = OffsetDateTime.now();
            }

            @Override
            public String toString() { return this.phrase; }
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
                this.usedPhrases = new ArrayList<>();
                this.caughtPhrases = new ArrayList<>();
            }

        }

        private static final int MIN_PLAYERS = 2;

        private final long callMessageLimit;

        public SecretePhrase(Weebot bot, User author, long callMessageLimit) {
            super(bot, author);
            this.callMessageLimit = callMessageLimit;
        }

        public SecretePhrase(Weebot bot, User author, long callMessageLimit,
                             User...players) {
            super(bot, author);
            this.callMessageLimit = callMessageLimit;
            for (User u : players) {
                this.PLAYERS.putIfAbsent(u.getIdLong(), new SPPlayer(u));
            }
        }

        @Override
        public void accept(BetterMessageEvent event) {
            if (event.getType() != BetterMessageEvent.TYPE.RECIVED) return;
            User member = event.getAuthor();
            SPPlayer memberPlayer = this.PLAYERS.get(member.getIdLong());
            if(memberPlayer != null)
                this.usePhrase(memberPlayer, event.getMessage());
        }

        /**
         * Checks if a player used their phrase
         * @param player The player
         * @param message The message sent by the user
         */
        protected final void usePhrase(SPPlayer player, Message message) {
            String string = message.getContentStripped();
            for (Phrase p : player.unusedPhrases) {
                if(string.contains(p.phrase)) {
                    player.unusedPhrases.remove(p);
                    player.usedPhrases.add(p);
                    return;
                }
            }
        }

        @Override
        public final boolean addUser(User user) {
            return this.joinGame(new SPPlayer(user));
        }

        /**
         * Adds {@code player} to games and generates phrases for them.
         *
         * @param player
         *         Player to add
         *
         * @return -1 if player could not be added 0 if player is already in Game 1 if player was added
         */
        @Override
        protected boolean joinGame(SPPlayer player) {
            if (super.joinGame(player)) {
                //TODO Variable Phrase number
                player.unusedPhrases.addAll(Arrays.asList(this.generatePhrases(5)));
                return true;
            } else
                return false;
        }

        @Override
        protected boolean startGame() {
            if (this.PLAYERS.values().size() < MIN_PLAYERS) {
                return false;
            }
            this.RUNNING = true;
            return true;
        }

        /**
         * End the game and decide a winner.
         * @return boolean for some reason //TODO
         */
        @Override
        protected boolean endGame() {
            return true;
        }

        //Member Make a game

        //Players join

        //Author starts game

        //Game
        //player says phrase
        //If other player calls out phrase exactly, point removed (or added?)

        //Time Runs out or Author ends game

        /**
         * Generate "random" (?) phrases to give to a {@link SPPlayer player}
         * in a private channel.
         * TODO How to generate phrases?
         * @param phrases
         *         The number of phrases to generate.
         *
         * @return An {@link String string} array of the param size filled with phrases.
         */
        private final Phrase[] generatePhrases(int phrases) {
            Phrase[] out = new Phrase[phrases];
            for (int i = 0; i < phrases; i++) {
                out[i] = new Phrase("Test Phrase");
            }
            return out;
        }

    }

    public SecretePhraseCommand() {
        super(
                "SecretePhrase",
                new ArrayList<>(Arrays.asList("sphrase")),
                "A game designed by Dernst",
                "", //TODO argformat
                true,
                false,
                0,
                false,
                false
        );
    }

    /** Possible actions players can call directly */
    private enum ACTION {START, STOP, JOIN, CALL}

    /**
     * Performs a check then runs the command.
     *
     * @param bot
     *         The {@link Weebot} that called the command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    public void run(Weebot bot, BetterMessageEvent event) {
        if (this.check(event)) {
            Thread thread = new Thread(() -> this.execute(bot, event));
            thread.setName(bot.getBotId() + ":SecretePhraseCommand");
            thread.start();
        } else {
            event.reply("This game can only be played in open text channels " +
                    "(``@everyone`` has read & write permissions).");
        }
    }

    /**
     * Performs the action of the command.
     *
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        //Save the args
        String[] args = this.cleanArgs(bot, event.getArgs());
        if (args.length < 2) {
            event.reply("Please usen\n```start, join, callout, or end```\nto interact " +
                    "with Secrete Phrase games.");
            return;
        }

        //Get the action
        ACTION action = parseAction(args[1]);
        if (action == null) {
            event.reply("Sorry, '" +
                    String.join(" ", args)
                    + "' is not a command for the Secrete Phrase game. Please " +
                    "use:```\nstart, join, callout, or stop```"
            );
            return;
        }

        //Get the game being acted upon to
        SecretePhrase game = null;
        for (IPassive p : bot.getPassives()) {
            if (p instanceof SecretePhrase) {
                game = (SecretePhrase) p;
            }
        }

        switch (action) {
            case START:
                if (game != null) {
                    //Don't allow more than one game to run.
                    event.reply("There is already a game of SecretePhrase running in " +
                            "this server. End that game first before starting a new one" +
                            ".");
                    return;
                } else {
                    //If no game is running, start one.
                    bot.addPassive(new SecretePhrase(bot, event.getAuthor(), 10));
                    event.reply("A new Secrete Phrase game has been started by @"
                                + event.getMember().getEffectiveName() + "! Use ```"
                                + "sphrase join```\nTo join the game.");
                    break;
                }
            case STOP:
                if (game == null) {
                    event.reply("There is no game of Secrete Phrase running.");
                    return;
                } else {
                    game.endGame();
                    //TODO Show scores
                    bot.getPassives().remove(game);
                }
                break;
            case JOIN:
                if (game == null) {
                    event.reply("There is no Secrete Phrase game running. You can use```"
                            + "sphrase start``` to start a new game and ```sphrase " +
                            "join``` to join said game."
                    );
                    return;
                } else {
                    SecretePhrase.SPPlayer player =
                                new SecretePhrase.SPPlayer(event.getAuthor());
                    if (game.joinGame(player)) {
                        event.reply("@" + event.getMember().getEffectiveName()
                                + " has joined the Secrete Phrase game! Check your"
                                + " direct messages to see your secrete phrases.");
                        String out = "";
                        for (int i = 0; i < player.unusedPhrases.size(); i++) {
                            out = out.concat(
                                    (i + 1) + ".)" + player.unusedPhrases.get(i) + "\n");
                        }
                        event.privateReply("```Secrete Phrases Game in "
                                + event.getGuild().getName() + ":\n" + out + "```");
                    } else {
                        event.reply("You are already in the Secrete Phrase game!");
                        return;
                    }
                }
                break;
            case CALL:
                //call @User Phrase Goes Here
                if (game == null) {
                    event.reply("There is no Secrete Phrase game running. You can use```"
                            + "sphrase start``` to start a new game and ```sphrase " +
                            "join``` to join said game."
                    );
                    return;
                } else {
                    System.out.println("CALL");
                }
                break;
        }
    }

    @Override
    protected final boolean check(BetterMessageEvent event) {
        if (event.getType() != BetterMessageEvent.TYPE.RECIVED) return false;
        //If it passes basic checks
        //Check if channel is @everyone
        if (super.check(event)) {
            //Get @everyone role (called publicRole by API)
            Role everyoneRole = event.getGuild().getPublicRole();
            //Get the channel of the message
            TextChannel channel = event.getTextChannel();
            //Get the permission overrides
            PermissionOverride override = channel.getPermissionOverride(everyoneRole);
            //Get the permissions
            if (override != null) {
                List<Permission> denied = override.getDenied();
                if (denied.isEmpty()) {
                    return true;
                } else if (denied.contains(Permission.MESSAGE_READ)
                            && denied.contains(Permission.MESSAGE_WRITE)) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Parse an {@link ACTION action} from a string.
     * @param action The string to parse from.
     * @return The action connected to the given string.
     *          null if no action could be parsed.
     */
    private static final ACTION parseAction(String action) {
        switch (action.toLowerCase()) {
            case "start":
                return ACTION.START;
            case "join":
                return ACTION.JOIN;
            case "call":
            case "callout":
                return ACTION.CALL;
            case "stop":
            case "end":
                return ACTION.STOP;
            default:
                return null;
        }
    }

}
