package com.ampro.weebot.commands.fun;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import javax.annotation.Nonnull;
import javax.naming.InvalidNameException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.dv8tion.jda.core.entities.Role;

/**
 * A Controller command for a server's custom {@link MemeReader.Meme memes}.
 * //TODO add to help
 * @author Jonathan Augustine
 */
public class CustomMemeCommand extends Command {

    /**
     * A passive listener for meme calls mimicking Discord's built-in ":xxx:"
     * format for calling emoji.
     */
    public static final class MemeReader implements IPassive {

        /**
         * A Meme object reflects a URL passed to Weebot by a user. All Memes
         * are named for easy usage and can be restricted to specific roles.
         */
        private static final class Meme {
            /** URL of the meme image */
            private final URL url;
            /** Name used to call the meme */
            private final String name;
            /**
             * The {@link Role roles} allowed to use this meme.
             * <br> {@link Nonnull}
             */
            private final List<Long> allowedRoles;

            /**
             * Make a meme. Requires a URL, name, and collection of roles (can be null)
             * @param url The url to the meme image
             * @param name The name of this meme
             * @param allowedRoles Roles allowed to use this meme
             */
            Meme(@Nonnull URL url, @Nonnull String name, Collection<Role> allowedRoles) {
                this.url  = url;
                this.name = name.toLowerCase();
                this.allowedRoles = new ArrayList<>();
                if (allowedRoles != null && !allowedRoles.isEmpty()) {
                    allowedRoles.forEach(r -> this.allowedRoles.add(r.getIdLong()));
                }
            }

        }

        private boolean dead;

        private final char[] restrictedChars = new char[] {'.', ':', ',', '*', '_',
                '~', '?', '\'', '\"', '(', ')'};

        /** Char used to denote a meme usage. */
        private char callSymbol;
        /** All memes held by this guild */
        private final List<Meme> memeList;

        /**
         * Build a MemeReader with a custom call symbol.
         * @param symbol The symbol to indicate a meme usage.
         * @throws javax.naming.InvalidNameException If the char is any
         *          {@link MemeReader#restrictedChars} or a letter.
         */
        MemeReader(char symbol) throws InvalidNameException {
            if (!allowedChar(symbol))
                throw new InvalidNameException("Cannot use Symbol [" + symbol + "]");
            this.memeList   = new ArrayList<>();
            this.callSymbol = symbol;
        }

        /**
         * Check if a char matches any of {@link MemeReader#restrictedChars} or
         * any (Latin) letter.
         * @param symbol The symbol to check.
         * @return {@code false} if the param matches and restricted char,
         *          letter or number
         */
        private boolean allowedChar(char symbol) {
            for (char c : restrictedChars) {
                if (symbol == c) return false;
            }
            return String.valueOf(symbol).matches("[A-z0-9]");
        }

        @Override
        public void accept(Weebot bot, BetterMessageEvent event) {
            String content = event.toString();
            int index = content.indexOf(String.valueOf(callSymbol)) + 1;
            String meme = "";
            for (int i = index; i < content.length(); i++) {
                if (content.charAt(i) == callSymbol) break;
                meme += content.charAt(i);
            }

        }

        /** @return {@code false} if the passive is no longer active */
        @Override
        public boolean dead() {
            return false;
        }

    }


    public CustomMemeCommand() {
        super(
                "CustomMeme",
                new String[]{"cmc"},
                "Manage Custom Server memes & emoji",
                "",
                true,
                false,
                0,
                false,
                false
        );
    }

    private enum ACTION {ADD, REMOVE, SEE, SEEALL}

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {

    }

    @Override
    public MessageEmbed getEmbedHelp() {
        EmbedBuilder eb = Launcher.getStandardEmbedBuilder();

        return eb.build();
    }
}
