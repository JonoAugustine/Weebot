package com.ampro.weebot.commands.fun;

import com.ampro.plib.exceptions.DuplicateNameException;
import com.ampro.plib.exceptions.InvalidNameException;
import com.ampro.weebot.Launcher;
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
        private final class Meme {
            /** URL of the meme image */
            private final String url;
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
             * @throws DuplicateNameException If a meme with the same name already exists
             */
            Meme(@Nonnull String url, @Nonnull String name, Collection<Role> allowedRoles)
            throws DuplicateNameException {
                this.url  = url;
                this.name = name.toLowerCase().replace(" ", "_");
                if (MemeReader.this.memeList.containsKey(this.name))
                    throw new DuplicateNameException(name);
                this.allowedRoles = new ArrayList<>();
                if (allowedRoles != null) {
                    allowedRoles.forEach(r -> this.allowedRoles.add(r.getIdLong()));
                }
            }

            /** @return The meme as an embed */
            MessageEmbed toEmbed() {
                return toEmbedBuilder().build();
            }

            /** @return The meme as an embed builder (For changing authors) */
            EmbedBuilder toEmbedBuilder() {
                return Launcher.getStandardEmbedBuilder()
                                          .setTitle("Weebot Memer: " + name, url)
                                          .setImage(url);
            }

        }

        private boolean dead;

        private static final char[] restrictedChars = new char[]
                {'.', ':', ',', '*', '_', '~', '?', '\'', '\"', '(', ')','{', '}', '`'};

        /** Char used to denote a meme usage. */
        private char callSymbol;
        /** All memes held by this guild */
        private final ConcurrentHashMap<String, Meme> memeList;

        /**
         * Build a MemeReader with a custom call symbol.
         * @param symbol The symbol to indicate a meme usage.
         * @throws javax.naming.InvalidNameException If the char is any
         *          {@link MemeReader#restrictedChars} or a letter.
         */
        MemeReader(String symbol) throws InvalidNameException {
            if (symbol.length() > 1 || !allowedChar(symbol.charAt(0)))
                throw new InvalidNameException("Cannot use Symbol [" + symbol + "]");
            this.memeList   = new ConcurrentHashMap<>();
            this.callSymbol = symbol.charAt(0);
        }

        /**
         * Initialize a Memereader with the default symbol: ';'
         */
        MemeReader() {
            this.callSymbol = ';';
            this.memeList   = new ConcurrentHashMap<>();
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

        /**
         * Add a new {@link Meme} to the memelist.
         * @param url The url to the meme image
         * @param name The name of this meme
         * @param allowedRoles Roles allowed to use this meme
         * @throws DuplicateNameException If a meme with the same name
         *              already exists
         */
        void addMeme(@Nonnull String url, @Nonnull String name,
                     Collection<Role> allowedRoles)
        throws DuplicateNameException {
            Meme meme = new Meme(url, name, allowedRoles);
            this.memeList.put(name, meme);
        }

        /**
         * Read the message and try and find a meme call between 2
         * {@link MemeReader#callSymbol}
         * @param bot The weebot who called
         * @param event The event to receive.
         */
        @Override
        public void accept(Weebot bot, BetterMessageEvent event) {
            Member member = event.getMember();
            String content = event.toString();
            int index = content.indexOf(String.valueOf(callSymbol)) + 1;
            String name = "";
            for (int i = index; i < content.length(); i++) {
                if (content.charAt(i) == callSymbol) break;
                name += content.charAt(i);
            }
            Meme meme = memeList.get(name);
            if (meme == null) {
                return;
            }
            event.reply(
                    meme.toEmbedBuilder()
                        .setAuthor(member.getEffectiveName(), null,
                                   member.getUser().getAvatarUrl())
                        .build()
            );
        }

        /** @return {@code false} if the passive is no longer active */
        @Override
        public boolean dead() {
            return dead;
        }

    }

    public CustomMemeCommand() {
        super(
                "CustomMeme",
                new String[]{"cmc"},
                "Manage Custom Server memes & emoji",
                "<action> /url/",
                true,
                false,
                0,
                false,
                false
        );
    }

    private enum Action {INIT, ADD, REMOVE, SEEALL}

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        //<action> <name> /URL/
        String[] args   = cleanArgs(bot, event);
        Action action   = parseAction(args[1]);
        if (action == null) {
            event.reply("*Please provide an action. ```help cmc```*");
            return;
        }
        MemeReader memeReader = null;
        for (IPassive iPassive : bot.getPassives()) {
            if (iPassive instanceof MemeReader) {
                memeReader = (MemeReader) iPassive;
            }
        }
        Message message = event.getMessage();
        String name;

        switch (action) {
            case INIT:
                //cmc init [symbol]
                if (memeReader == null) {
                    if (args.length == 3) {
                        try {
                            memeReader = new MemeReader(args[2]);
                        } catch (InvalidNameException e) {
                            event.reply("*" + e + "*");
                            break;
                        }
                    } else {
                        memeReader = new MemeReader();
                    }
                    bot.addPassive(memeReader);
                    StringBuilder sb = new StringBuilder()
                            .append("*The MemeReader has been initialized!")
                            .append(" You can use your memes like this*:```")
                            .append(memeReader.callSymbol).append("meme_Name")
                            .append(memeReader.callSymbol).append("```");
                    event.reply(sb.toString());
                } else {
                    event.reply("*The meme reader has already been initialized*");
                }
                break;
            case ADD:
                //cmc add <name> <url> [@Role]...
                if (memeReader != null) {
                    if (args.length == 4) {
                        MemeReader.Meme meme = null;
                        try {
                            memeReader.addMeme(
                                    args[3], args[2], message.getMentionedRoles()
                            );
                        } catch (DuplicateNameException e) {
                            StringBuilder sb = new StringBuilder()
                                    .append("*A meme with the same name")
                                    .append(" already exists.*");
                            event.reply(sb.toString());
                            break;
                        }
                        event.reply("*The meme* \"" + meme.name + "\" *was added*.");
                    } else {
                        StringBuilder sb = new StringBuilder()
                                .append("*Please use the correct format to")
                                .append(" add a meme.*")
                                .append("```cmc add <name> <url>```");
                        event.reply(sb.toString());
                    }
                } else {
                    event.reply("*Please initialize the meme reader.*```help cmc```");
                }
                break;
            case REMOVE:
                break;
            case SEEALL:
                break;
        }

    }

    /**
     * Parse an {@link Action} from a String
     * @param arg The string to parse from
     * @return An action parsed from the string, null if non found
     */
    private Action parseAction(String arg) {
        switch (arg.toLowerCase()) {
            case "init":
                return Action.INIT;
            case "add":
                return Action.ADD;
            case "remove":
            case "rm":
                return Action.REMOVE;
            case "seeall":
            case "see":
                return Action.SEEALL;
            default:
                return null;
        }
    }

    @Override
    public MessageEmbed getEmbedHelp() {
        EmbedBuilder eb = Launcher.getStandardEmbedBuilder();
        //TODO embed help
        return eb.build();
    }
}
