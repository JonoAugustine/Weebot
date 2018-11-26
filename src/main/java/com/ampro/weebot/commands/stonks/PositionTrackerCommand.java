package com.ampro.weebot.commands.stonks;

import com.ampro.weebot.Launcher;
import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.commands.properties.Restriction;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import com.ampro.weebot.util.Unicode;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.BuySell.BUY;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.BuySell.SELL;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.Security.CALL;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.Security.PUT;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.TimeFrame.DAY_TRADE;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.TimeFrame.SWING;
import static jdk.nashorn.internal.objects.NativeArray.join;
import static net.dv8tion.jda.core.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.core.Permission.MANAGE_CHANNEL;

/**
 * A Command to control the Weebot stock-market position tracking features
 *
 * @author Jonathan Augustine
 * @since 1.1.0
 */
public class PositionTrackerCommand extends Command {

    /**
     * A {@link PositionTracker} passively watches a {@link TextChannel}
     * for security entry and exit reports. The tracker needs a minimum amount
     * of information in order to track a new position.<br>
     * A tracked position submission should follow this pattern: [x] x is optional <br>
     *
     * {@code in/out [$]TICKER [$]targetPrice type expiration/date [@]entryPrice
     * [OrderSize] [day|Swing] [reason]}
     *
     */
    public static final class PositionTracker implements IPassive {

        public enum BuySell {BUY, SELL}

        public enum Side {LONG, SHORT}

        public enum Security {CALL, PUT, STOCK}

        public enum TimeFrame {SWING, DAY_TRADE}

        public abstract static class Position implements Comparable<Position> {

            final String ticker;
            final BuySell buySell;
            final double entryPrice;
            double exitPrice = -1;
            final double orderSize;
            TimeFrame timeFrame;
            String reason;
            boolean open = true;

            Position(String ticker, BuySell buySell, double entryPrice,
                     double orderSize, TimeFrame timeFrame, String reason) {
                this.ticker     = ticker.toUpperCase();
                this.buySell    = buySell;
                this.entryPrice = entryPrice;
                this.orderSize  = orderSize;
                this.timeFrame  = timeFrame;
                this.reason     = reason;
            }

            Position(String ticker, BuySell buySell, double entryPrice,
                     double orderSize, String reason) {
                this.ticker     = ticker.toUpperCase();
                this.buySell    = buySell;
                this.entryPrice = entryPrice;
                this.orderSize  = orderSize;
                this.reason     = reason;
            }

            Position(String ticker, BuySell buySell, double entryPrice,
                     double orderSize, TimeFrame timeFrame) {
                this.ticker     = ticker.toUpperCase();
                this.buySell    = buySell;
                this.entryPrice = entryPrice;
                this.orderSize  = orderSize;
                this.timeFrame  = timeFrame;
            }


            /**
             * Checks if {@link Position} is equal in {@link BuySell},
             * {@link String ticker}, {@link TimeFrame}, entry price and
             * order size.
             *
             * @param other The {@link Position} to compare
             * @return true if the criteria is met
             */
            public boolean equal(Position other, boolean buysell) {
                return other.getClass() == this.getClass()
                        && other.ticker.equalsIgnoreCase(ticker)
                        && (!buysell || other.buySell == buySell)
                        && other.entryPrice == entryPrice
                        && other.orderSize == orderSize
                        && other.timeFrame == timeFrame;
            }

            /**
             * Checks if {@link Position} is equal in {@link BuySell},
             * {@link String ticker}, {@link TimeFrame}, entry price and
             * order size.
             *
             * @param other The {@link Position} to compare
             * @return true if the criteria is met
             */
            public boolean equal(Position other) {
                return equal(other, false);
            }

        }

        /**
         * TODO
         */
        public final class StockPostion extends Position {

            public StockPostion(String ticker, BuySell buySell, float entryPrice,
                                float orderSize, TimeFrame timeFrame, String reason) {
                super(ticker, buySell, entryPrice, orderSize, timeFrame, reason);

            }

            @Override
            public int compareTo(Position position) {
                return 0;
            }
        }

        public final class OptionPosition extends Position {

            final double targetPrice;
            final int[] expiration;  // m/d/y
            final Security security;

            OptionPosition(String ticker, BuySell buySell, double entryPrice,
                           Security callPut, double targetPrice, int[] expiration,
                           double orderSize, TimeFrame timeFrame, String reason) {
                super(ticker, buySell, entryPrice, orderSize, timeFrame, reason);
                this.targetPrice = targetPrice;
                this.expiration = expiration;
                this.security = callPut;
            }

            OptionPosition(String ticker, BuySell buySell, double entryPrice,
                           Security callPut, double targetPrice, int[] expiration,
                           double orderSize, String reason) {
                super(ticker, buySell, entryPrice, orderSize, reason);
                this.targetPrice = targetPrice;
                this.expiration = expiration;
                this.security = callPut;
            }

            @Override
            public boolean equal(Position other) {
                return super.equal(other)
                        && ((OptionPosition) other).targetPrice == targetPrice
                        && ((OptionPosition) other).expiration == expiration;
            }

            @Override
            public String toString() {
                return buySell + " " + ticker + " $" + targetPrice + " "
                        + security + expiration[0] + "/" + expiration[1]
                        + (expiration[2] == 0 ? "" : "/" + expiration[2])
                        + " @$" + entryPrice + (orderSize>1 ? "x"+orderSize :"")
                        + (reason != null ? " " + reason : "")
                        + (!open ? "\t>>closed @$" + exitPrice + " for "
                        + Math.round(((exitPrice - entryPrice) / entryPrice * 100)
                                             * 100.0) / 100.0 + "%" : "");
            }

            @Override
            public int compareTo(Position position) {
                if (this.exitPrice == -1) {
                    if (position.exitPrice == -1) {
                        return 0;
                    } else return 1;
                } else {
                    if (position.exitPrice == -1) {
                        return 1;
                    } else {
                        return (int) ((exitPrice - entryPrice) / entryPrice * 100)
                                - (int) ((position.exitPrice - position.entryPrice)
                                / position.entryPrice * 100);
                    }
                }
            }
        }

        /** Restrictions of Tracker Modification */
        final Restriction modRestriction = new Restriction();

        /** The TextChannel to track within */
        long channelID;

        /** The name of the tracker (optional) */
        String name;

        /** When the tracking began */
        OffsetDateTime start;

        Security defaultType = CALL;

        /** Map of user IDs to {@link ArrayList} of {@link Position} */
        final ConcurrentHashMap<Long, ArrayList<OptionPosition>> userPositionMap;

        /** Whether this {@link IPassive} is set to be destroyed on the next scan */
        boolean deathRow = false;

        /**
         * Build a {@link PositionTracker} for a {@link TextChannel}.
         *
         * @param channel   The Channel to track within
         * @param trackerName A name for the tracker
         */
        public PositionTracker(TextChannel channel, String trackerName) {
            this.channelID = channel.getIdLong();
            this.name = trackerName;
            this.start = OffsetDateTime.now();
            this.userPositionMap = new ConcurrentHashMap<>();
        }

        /**
         * Read a message and attempt to parse a {@link Position} from it.
         * <br>
         * Sample:
         * <br>
         *     in MSFT $100.00 call 9/21 @$1.11 2 swing
         *
         * @param bot The weebot who called
         * @param event The event to receive.
         */
        @Override
        public void accept(Weebot bot, BetterMessageEvent event) {
            ArrayList<String> args = new ArrayList<>(Arrays.asList(cleanArgs(bot, event)));
            Message m = event.getMessage();
            String message = String.join(" ", args).toLowerCase();


            //call|put expir/date [at|@][$]entry.price [orderSize] [description]

            if (args.size() < 5) {
                event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                return;
            }

            OptionPosition position = (OptionPosition) read(args, event);
            if (position == null) {return;}

            ArrayList<OptionPosition> list = new ArrayList<>();
            ArrayList<OptionPosition> list1 = userPositionMap
                    .putIfAbsent(event.getAuthor().getIdLong(), list);
            if (list1 != null) { list = list1; }
            list.removeIf(Objects::isNull);

            if (position.buySell == BUY) {
                list.add(list.size(), position);
            } else {
                outerloop: for (OptionPosition p : list) {
                    if (p.ticker.equals(position.ticker) && p.open
                            && p.buySell != position.buySell) {
                        for (int i = 0; i < p.expiration.length; i++) {
                            if (p.expiration[i] != position.expiration[i]) {
                                continue outerloop;
                            }
                        }
                        p.open = false;
                        p.exitPrice = position.entryPrice;
                        //We don't sort here so that the most recently opened
                        // position is assumed to be the one being closed
                    }
                }
            }

        }

        /**
         * Attempt to generate a {@link Position} from a message.
         *
         * @param args
         * @param event
         * @return A generated {@link Position} or null
         */
        private Position read(ArrayList<String> args, BetterMessageEvent event) {
            // in/out
            BuySell bySl;
            String bs = args.remove(0);
            if (bs.matches("(i+n*)|(b+u*y*)")) {
                bySl = BUY;
            } else if (bs.matches("(o+u*t*)|(s+e*l*l*)")){
                bySl = SELL;
            } else {
                event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                return null;
            }

            // [$]TICKER
            String tkr = args.remove(0).replaceAll("[^A-Za-z]", "");
            if (tkr.isEmpty()) {
                //No ticker, do something about it TODO
                event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                return null;
            }

            // [$]targetPrice
            String targetPrice = args.remove(0);
            Security security = null;
            double targetDouble;
            if (targetPrice.matches("^\\$?\\d*(\\.\\d*)?(c+(a*l*l*)|p+(u*t*))?$")) {
                if (targetPrice.matches("(?s).*p+u*t*(?s).*")) {
                    security = PUT;
                } else if (targetPrice.matches("(?s).*c+a*l*l*(?s).*")) {
                    security = CALL;
                }
                targetPrice = targetPrice.replaceAll("[^0-9.]", "");//Clear non-digits

                try { //Attempt to parse the price Double from the string
                    targetDouble = Double.parseDouble(targetPrice);
                } catch (NumberFormatException e) {
                    //TODO could not parse a double
                    event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                    System.out.println(targetPrice);
                    return null;
                }
            } else {
                //Something not good TODO
                event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                return null;
            }

            // call or put
            if (security == null) { //If the call/put was not found in the last one
                String sec = args.remove(0);
                if (sec.matches("p+u*t*")) {
                    security = PUT;
                } else if (sec.matches("c+a*l*l*")) {
                    security = CALL;
                } else {
                    //Something not good TODO
                    event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                    return null;
                }
            }

            // expiration/date
            int[] exp = new int[3];
            String expiration = args.remove(0);
            if (expiration.matches("^(1[0-2]|0*[1-9])[^0-9\\s+](([1-2][0-9])|(3[0-1])" +
                                           "|(0*[1-9]))([^0-9\\s+]\\d{0,4})?$")) {

                String[] dts = expiration.split("[^0-9\\s+]");
                if (dts.length > exp.length) {
                    //Something not good TODO
                    event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                    return null;
                }
                for (int i = 0; i < dts.length; i++) {
                    try {
                        exp[i] = Integer.parseInt(dts[i]);
                    } catch (NumberFormatException e) {
                        //Something not good TODO
                        event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                        return null;
                    }
                }
            } else {
                //Something not good TODO
                event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                return null;
            }

            // [at|@][$]entryPrice
            String at = args.remove(0);
            // 1st check if it is connected or not
            if (at.matches("^(a+(t*))|@+$")) { at = args.remove(0); }
            // Then check the price
            double entryPrice;
            if (at.matches("(?s).*\\$*\\d*(\\.\\d*)?(?s).*")) {
                at = at.replaceAll("[^0-9.]", "");//Clear non-digits
                try { //Attempt to parse the price Double from the string
                    entryPrice = Double.parseDouble(at);
                } catch (NumberFormatException e) {
                    //TODO could not parse a double
                    event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                    System.out.println(at);
                    return null;
                }
            } else {
                event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                System.out.println(at);
                return null;
            }

            // [orderSize]
            double size = 1;
            String reason = "";
            if (!args.isEmpty()) {
                String oSze = args.remove(0);
                if (oSze.matches("(?s).*[Xx]*\\d+(?s).")) {
                    try {
                        size = Double.parseDouble(oSze);
                        if (!args.isEmpty()) {
                            reason = args.remove(0);
                        }
                    } catch (NumberFormatException e) {
                        // [description]
                        reason = join(args, ' ');
                    }
                }
            }

            TimeFrame tf = null;
            if (reason.matches(".*s+w*i*n*g*.*")) {
                tf = SWING;
            } else if (reason.matches(".*d+((t+)|(ay(trade)?)).*")) {
                tf = DAY_TRADE;
            }

            OptionPosition position = new OptionPosition(tkr, bySl, entryPrice,
                                                         security, targetDouble,
                                                         exp, size, tf, reason);

            event.getMessage().addReaction(Unicode.HEAVY_CHECK.val).queue();

            return position;

        }

        MessageEmbed toEmbed(Guild guild, List<Member> members) {
            if (members.isEmpty()) {
                return toEmbed(guild);
            }
            EmbedBuilder eb = Launcher.getStandardEmbedBuilder();

            StringBuffer sb = new StringBuffer();

            eb.setThumbnail(guild.getIconUrl())
              .setTitle(guild.getName() + " Market Positions");

            sb.append("Options Market Positions held or closed by the members")
              .append(" mentioned");
            eb.setDescription(sb.toString());
            sb.setLength(0);

            ArrayList<Long> killList = new ArrayList<>(userPositionMap.size());

            members.forEach( member -> {
                ArrayList<OptionPosition> list = userPositionMap.get(member.getUser()
                                                                     .getIdLong());
                if (list == null) {
                    killList.add(member.getUser().getIdLong());
                    return;
                }
                list.removeIf(Objects::isNull);

                double entries = 0;
                double exits = 0;
                for (Position position : list) {
                    if (!position.open) {
                        entries += position.entryPrice;
                        exits += position.exitPrice; //Add average returns
                    }
                    sb.append(">").append(position.toString()).append("\n");
                }
                double up = ((exits - entries) / entries * 100);
                String tit = member.getEffectiveName() + " " +
                        (up > 0 ? Unicode.UP_BTTON.val
                                : (int) up < 0 ? Unicode.DOWN_BUTTON.val
                                : Unicode.PAUSE_BUTTON.val) + " "
                        + Math.round(up * 100.0) / 100.0 + "%";
                eb.addField(tit, sb.toString(), false);
                sb.setLength(0);
            });

            killList.forEach(userPositionMap::remove);

            return eb.build();
        }

        MessageEmbed toEmbed(Guild guild) {
            EmbedBuilder eb = Launcher.getStandardEmbedBuilder();
            StringBuffer sb = new StringBuffer();

            eb.setThumbnail(guild.getIconUrl())
              .setTitle(guild.getName() + " Market Positions");

            sb.append("Options Market Positions held or closed by members in")
              .append(" " + guild.getTextChannelById(channelID).getAsMention());
            eb.setDescription(sb.toString());
            sb.setLength(0);

            ArrayList<Long> killList = new ArrayList<>(userPositionMap.size());

            userPositionMap.forEach( (id, list)-> {
                list.removeIf(Objects::isNull);
                Member member = guild.getMemberById(id);
                if (member == null) {
                    killList.add(id);
                    return;
                }
                double entries = 0;
                double exits = 0;
                for (Position position : list) {
                    if (!position.open) {
                        entries += position.entryPrice;
                        exits += position.exitPrice;
                    }
                    sb.append(position.toString()).append("\n");
                }
                double up = ((exits - entries) / entries * 100);
                String tit = member.getEffectiveName() +
                        (up > 0 ? Unicode.UP_BTTON.val
                                : (int) up < 0 ? Unicode.DOWN_BUTTON.val
                                : Unicode.PAUSE_BUTTON.val) + " "
                        + Math.round(up * 100.0) / 100.0 + "%";
                eb.addField(tit, sb.toString(), false);
                sb.setLength(0);
            });

            killList.forEach(userPositionMap::remove);

            return eb.build();
        }

        @Override
        public boolean dead() { return deathRow; }

    }

    /** Build a Command with each variable assigned. */
    public PositionTrackerCommand() {
        super("PositionTracker", new String[]{"ptc", "position", "trades"},
              "Track stock and option positions.",
              "in/out TICKER  [$]strike.price call|put expir/date [at|@][$]entry.price [orderSize] [description]",
              true, false, 0, false, false);
        this.userPermissions = new Permission[] {ADMINISTRATOR, MANAGE_CHANNEL};
    }

    private enum Action { INIT, CLOSE, SEE}

    @Override
    protected void execute(Weebot bot, BetterMessageEvent event) {
        String[] args = cleanArgs(bot, event);
        Message m = event.getMessage();
        TextChannel channel = event.getTextChannel();

        ArrayList<PositionTracker> trackers = bot.getPassives(PositionTracker.class);
        PositionTracker chTracker = null;
        for (PositionTracker tracker : trackers) {
            if (tracker.channelID == event.getTextChannel().getIdLong()) {
                chTracker = tracker;
            }
        }

        Action action = Action.SEE;
        if (args.length > 1) {
            action = parseAction(args[1]);
        }

        switch (action) {
            case INIT: //ptc init [name]
                if (chTracker == null) {
                    String name;
                    if (args.length == 3) {
                        name = args[2];
                    } else {
                        name = channel.getName() + " Position Tracker";
                    }
                    bot.addPassiveM(new PositionTracker(channel, name));
                    event.reply("Your PositionTracker has been initiated! *May the Tendies be with you.*");
                    event.getMessage().addReaction(Unicode.HEAVY_CHECK.val).queue();
                } else {
                    event.reply("There is already a Position Tracker in this channel. Use ``ptc close`` to close it.");
                }

                return;

            case CLOSE:
                if (chTracker != null) {
                    chTracker.deathRow = true;
                }
                break;
            case SEE: //ptc [any...thing] [#channel] [@user]
            default: //ptc
                List<TextChannel> channels = event.getMessage().getMentionedChannels();
                ArrayList<Member> members = new ArrayList<>(
                        event.getMessage().getMentionedMembers());
                members.removeIf(member -> member.getUser().isBot());

                if (channels.isEmpty()) { //if no args, show the current channel pt
                    if (chTracker != null) {
                        event.reply(chTracker.toEmbed(event.getGuild(), members));
                    } else {
                        event.reply("There is no active Position Tracker in this channel.");
                        //todo give help
                    }

                } else {
                    ArrayList<TextChannel> nons = new ArrayList<>(trackers.size());
                    channels.forEach( ch -> {
                        for (PositionTracker tracker : trackers) {
                            if (tracker.channelID == ch.getIdLong()) {
                                event.reply(tracker.toEmbed(event.getGuild(), members));
                                return;
                            }
                        }
                        nons.add(ch);
                    });
                    if (!nons.isEmpty()) {
                        StringBuffer sb = new StringBuffer()
                                .append("The following channels have no Position ")
                                .append("Tracker enabled: ");
                        nons.forEach(ch -> sb.append(ch.getAsMention()).append(" "));
                        event.reply(sb.toString());
                    }
                }
                break;
        }
    }

    /**
     * Try and parse an action from arguments.
     *
     * @param arg The arg to parse.
     * @return The {@link Action} parsed or null if no action was found
     */
    private Action parseAction(String arg) {
        switch (arg.toLowerCase()) {
            case "init":
            case "live":
            case "open":
            case "start":
                return Action.INIT;
            case "end":
            case "close":
                return Action.CLOSE;
            default: return Action.SEE;
        }
    }


    @Override
    public MessageEmbed getEmbedHelp() {
        return super.getEmbedHelp();//TODO
    }
}
