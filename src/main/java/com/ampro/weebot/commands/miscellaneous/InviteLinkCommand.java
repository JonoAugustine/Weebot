package com.ampro.weebot.commands.miscellaneous;

import com.ampro.weebot.commands.Command;
import com.ampro.weebot.entities.bot.Weebot;
import com.ampro.weebot.listener.events.BetterMessageEvent;

public class InviteLinkCommand extends Command {

    public static final String INVITE_LINK =
            "https://discordapp.com/oauth2/" +
                    "authorize?client_id=437851896263213056&scope=bot";


    public InviteLinkCommand() {
        super(
                "InviteLink",
                new String[]{"inviteme", "ilc"},
                "*Get a link to invite me to your sever.*",
                "",
                false,
                false,
                0,
                false,
                false
        );
    }

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        StringBuilder sb = new StringBuilder("You can invite me to your server")
                .append(" at this link: ").append(INVITE_LINK)
                .append(" . Hope to see you there! :smiley:");
        event.reply(sb.toString());
    }

}
