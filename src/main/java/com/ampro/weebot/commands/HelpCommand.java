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
                , false
        );
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
        StringBuilder sb = new StringBuilder();
        sb.append("**Settings**\n").append("```")
          .append("- Set bot nickname with 'setname <name>'\n")
          .append("- Change the prefix with 'prefix <new_prefix>'\n")
          .append("- (Dis)Allow the bot to use explicit language")
          .append(" with 'expl on/off'\n")
          .append("- Allow or disallow the bot to use NSFW commands")
          .append(" with 'nsfw of/off'\n")
          .append("- Server-wide word bans. (Under construction)")
          .append("- (Dis)Allow the bot to respond to actions not directed to it.")
          .append("(Under construction)\n\n")
          .append("```");
        event.privateReply(sb.toString());

        sb.setLength(0);

        sb.append("**Commands**\nAsk this for more details```help [command_name]```.\n")
          .append("```")
          .append("- Note Pads ('notes')\n")
          .append("\t- Write and edit Note Pads\n")
          .append("\t- Lock Note Pads to roles, members, and text channels\n")
          .append("- Reminders (Under construction)\n")
          .append("- Cards Against Humanity ('cah')\n")
          .append("\t- Official decks up to Expansion 3\n")
          .append("\t- Sever Custom Decks\n")
          .append("- Secrete Phrase (Under construction)\n")
          .append("- Self-destruct messages 'deleteme'\n")
          .append("- List all guilds hosting a Weebot 'listguilds'\n")
          .append("- Ping 'ping'")
          .append("```");
        event.privateReply(sb.toString());

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

    @Override
    public String getHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("Help Help...wait what?");
        return sb.toString();
    }

}
