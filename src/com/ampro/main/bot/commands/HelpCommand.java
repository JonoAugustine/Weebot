package com.ampro.main.bot.commands;

import com.ampro.main.bot.Weebot;
import com.ampro.main.listener.events.BetterMessageEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 *
 */
public class HelpCommand extends Command {

    public HelpCommand() {
        super(
                "Help"
                , new ArrayList<String>(
                        Arrays.asList("helpme", "showhelp")
                )
                , "Show how to interact with me."
                , "[command]"
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
     * Performs the action of the command.
     *
     * @param bot
     *         The {@link Weebot} which called this command.
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        switch (new Random().nextInt() % 2) {
            case 0:
                event.reply("Check your DMs hot stuff :stuck_out_tongue_winking_eye:");
                event.privateReply("Think of me :smirk: https://bit.ly/1LnkxHw");
                break;
            default:
                event.reply("No amount of help will ever be enough to fix the" +
                        " pathetic life you've made for yourself.");
        }
    }

    /**
     * Performs the action of the command.
     *
     * @param event
     *         The {@link BetterMessageEvent} that called the command.
     */
    @Override
    protected void execute(BetterMessageEvent event) {}

}
