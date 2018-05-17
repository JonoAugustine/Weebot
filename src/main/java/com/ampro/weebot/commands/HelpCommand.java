package com.ampro.weebot.commands;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A {@link Command} to send information and help to the use requesting it.
 *
 * @author Jonathan Augustine
 */
public class HelpCommand extends Command {

    public HelpCommand() {
        super(
                "Help"
                , new ArrayList<>(
                        Arrays.asList("helpme", "showhelp")
                )
                , "Show how to interact with me."
                , "<help> [command]"
                , false
                , false
                , 0
                , false
        );
    }

    public HelpCommand(String name, List<String> aliases, String help
                        , String argFormat, boolean guildOnly, boolean ownerOnly
                        , int cooldown, boolean hidden) {
        super(name, aliases, help, argFormat, guildOnly, ownerOnly, cooldown, hidden);
    }

    /**
     * Performs a check then runs the command in a new {@link Thread}
     *
     * @param bot
     *         The {@link Weebot} that called the command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    public void run(Weebot bot, BetterMessageEvent event) {
        if(this.check(event)) {
            Thread thread = new Thread(() -> this.execute(bot, event));
            thread.setName(bot.getBotId() + " : HelpCommand");
            thread.start();
        }
    }

    /**
     * Reads the command and arguments given and responds with the requested
     * information or help.
     *
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        /** Message arguments cleansed of the callsign or bot mention */
        String[] args = this.cleanArgs(bot, event);

        //If the only argument is the command invoke
        if (args.length == 1) {
            this.genericHelp(bot, event);
        } else {
            for (Command c : Launcher.getCommands()) {
                if(c.isCommandFor(args[1])) {
                    event.reply(c.getHelp());
                    return;
                }
            }
            if(event.getGuild().getName() == "Numberless Liquidators") {
                switch (new Random().nextInt() % 2) {
                    case 0:
                        event.reply("Check your DMs hot stuff :stuck_out_tongue_winking_eye:");
                    default:
                        event.privateReply("Think of me :smirk: https://bit.ly/1LnkxHw");
                }
            } else {
                event.reply("Help isn't on the way just yet...(under construction)");
            }
        }

    }

    /**
     * Send the generic help information about Weebot.
     * @param bot The bot who called.
     * @param event The event that invoked this.
     */
    private void genericHelp(Weebot bot, BetterMessageEvent event) {
        //TODO genericHelp response
        String out = "```" + bot.getNickname() + " Help:\n";
        out += "Sorry, this command is still under construction.\n";
        out += "```";
        event.reply(out);
    }

    /**
     * Send a list of all the {@link Command}s Weebots can perform
     * and how to use them.
     * @param bot The bot who called
     * @param event The event that invoked
     */
    private void commandsHelp(Weebot bot, BetterMessageEvent event) {
        //TODO commandsHelp
        String out = "```" + bot.getNickname() + " Commands:\n";
        out += "Sorry, this command is still under construction.\n";
        out += "```";
        event.reply(out);
    }

}
