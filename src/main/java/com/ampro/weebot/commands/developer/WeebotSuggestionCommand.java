package com.ampro.weebot.commands.developer;


import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * A way for anyone in a Guild hosting a Weebot to make suggestions to the
 * developers.
 */
public class WeebotSuggestionCommand extends Command {

    public static final class Suggestion {
        private final String suggestion;
        private final long authorID;
        private final long guildID;
        private final OffsetDateTime submitTime;

        public Suggestion(String suggestion, User author) {
            this.submitTime = OffsetDateTime.now();
            this.suggestion = suggestion;
            this.authorID = author.getIdLong();
            this.guildID = -1L;
        }

        public Suggestion(String suggestion, long authorID) {
            this.submitTime = OffsetDateTime.now();
            this.suggestion = suggestion;
            this.authorID = authorID;
            this.guildID = -1L;
        }

        public Suggestion(String suggestion, User author, Guild guild) {
            this.submitTime = OffsetDateTime.now();
            this.suggestion = suggestion;
            this.authorID = author.getIdLong();
            this.guildID = guild != null ? guild.getIdLong() : -1L;
        }

        public Suggestion(String suggestion, long authorID, long guildID) {
            this.submitTime = OffsetDateTime.now();
            this.suggestion = suggestion;
            this.authorID = authorID;
            this.guildID = guildID;
        }

        public long getAuthorID() { return this.authorID; }

        public long getGuildID() { return this.guildID; }

        public OffsetDateTime getSubmitTime() { return this.submitTime; }

        @Override
        public String toString() { return this.suggestion; }

    }

    public WeebotSuggestionCommand() {
        super(
                "Suggest",
                new ArrayList<>(Arrays.asList("suggestion", "sugg", "loadsuggs",
                        "seesuggs", "allsuggs")),
                "Submit a suggestion to the Weebot developers right from Discord!",
                "<suggest/suggestion/sugg> <Your Suggestion>",
                false,
                false,
                0,
                false,
                false
        );
    }

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
        Thread thread = new Thread(() -> this.execute(bot, event));
        thread.setName(bot.getBotId() + " : WeebotSuggestionCommand");
        thread.start();
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
        String[] args = this.cleanArgs(bot, event);
        switch (args[0]) {
            case "loadsuggs":
            case "seesuggs":
            case "allsuggs":
                if (isDev(event.getAuthor())) {
                    sendSuggestions(event);
                    return;
                } else {
                    event.reply("Sorry, but only developers can see suggestions at " +
                            "this time.");
                    return;
                }
            default:
                String sugg = String.join(" ",Arrays.copyOfRange(args,1,args.length))
                        .trim();
                Launcher.getDatabase()
                        .addSuggestion(new Suggestion(sugg, event.getAuthor(),
                                event.getGuild()));
                event.reply("Thank you for your suggestion! We're working hard to " +
                        "make Weebot as awesome as possible, but we " +
                        "will try our best to include your suggestion!");
                return;
        }
    }

    private void sendSuggestions(BetterMessageEvent event) {
        String out = "``` ";
        Collection<Suggestion> it = Launcher.getDatabase().getSuggestions().values();
        int i = 0;
        for (Suggestion s : it) {
            Guild g = Launcher.getGuild(s.guildID);
            out = out.concat(
                    s.submitTime.format(DateTimeFormatter.ofPattern("d-M-y hh:mm:ss"))
                            + " | by " + Launcher.getJda().getUserById(s.authorID).getName()
                            + " | on " + (g != null ? g.getName() : "Private Chat")
                            + "\n" + i++ + ") \"" + s + "\"\n\n"
            );
        }

        out += "```";
        if (it.size() > 50) {
            event.privateReply(out);
        } else
            event.reply(out);
    }

    private boolean isDev(User user) {
        return Launcher.checkDevID(user.getIdLong());
    }

    @Override
    public MessageEmbed getEmbedHelp() {
        EmbedBuilder eb = Launcher.getStandardEmbedBuilder();

        eb.setTitle("Weebot Suggestions")
          .setDescription("Submit suggestions to the Weebot dev team.");

        eb.addField("Submit Suggestion with this command",
                    "sugg <Your Suggestion>\n*Aliases: suggest, suggestion*",
                    false);

        return eb.build();
    }
}
