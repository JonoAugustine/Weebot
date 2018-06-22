package com.ampro.weebot.commands;

import com.ampro.weebot.bot.Chatbot;
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.entities.MessageEmbed;

/**
 * Initialize a Guild chatbot.
 * TODO add to help
 */
public class ChatbotCommand extends Command {

    public ChatbotCommand() {
        super(
                "Chatbot",
                new String[]{"cbc"},
                "Run a chatbot that will talk whenever wherever",
                "init/kill",
                true,
                false,
                0,
                false,
                false
        );
    }

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        String action = cleanArgsLowerCase(bot,event)[1];
        switch (action) {
            case "init":
            for (IPassive p : bot.getPassives()) {
                if(p instanceof Chatbot) {
                    event.reply("*There is already a chatbot running.*");
                    return;
                }
            }
            if(!bot.canParticipate()) {
                event.reply(
                        "*Active Participate must be activated to use this. Check "
                                + "``help`` for more information*.");

                return;
            }


            try {
                bot.addPassive(new Chatbot(event.getGuild()));
            } catch (Exception e) {
                event.reply(
                        "*Sorry, something went wrong settings up the Chatbot. Please "
                                + "try again later.*");
                return;
            }

            event.reply("The Chatbot is now active.");
            break;
            case "kill":
                bot.getPassives().removeIf( (IPassive p) -> p instanceof Chatbot );
                event.reply("*The Chatbot has been killed*");
        }
    }

    @Override
    public MessageEmbed getEmbedHelp() {
        return super.getEmbedHelp();
    }
}
