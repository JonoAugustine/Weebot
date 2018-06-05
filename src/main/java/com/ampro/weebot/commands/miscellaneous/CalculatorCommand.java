package com.ampro.weebot.commands.miscellaneous;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.mariuszgromada.math.mxparser.Expression;

import java.awt.*;
import java.util.Arrays;

/**
 * Basic calculator.
 */
public class CalculatorCommand extends Command {

    public CalculatorCommand() {
        super(
                "Calculator",
                new String[]{"calc", "cc", "lrc"},
                "Do some math",
                "A+B*C/D%E",
                false,
                false,
                0,
                false,
                false
        );
    }

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        String expression = clipCommand(bot, event);

        Expression e = new Expression(expression);

        String out = e.calculate() + "";

        event.reply(out.equalsIgnoreCase("nan") ?
                    "*Sorry, I coulnd'couldn't understand that.*" : out);

    }

    @Override
    public MessageEmbed getEmbedHelp() {
        EmbedBuilder eb = Launcher.getStandardEmbedBuilder().setColor(new Color(0x00BCFF));
        eb.setTitle("Calculator", "https://github.com/mariuszgromada/MathParser" +
                            ".org-mXparser/wiki/All-built-in-tokens");
        eb.setDescription("A calculator using mXparser");
        return eb.build();
    }
}
