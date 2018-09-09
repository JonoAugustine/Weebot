package com.ampro.weebot.commands.stonks;

import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.commands.properties.Restriction;
import com.ampro.weebot.commands.util.NotePadCommand;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import com.ampro.weebot.util.Unicode;
import javafx.geometry.Pos;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.BuySell.BUY;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.BuySell.SELL;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.Security.CALL;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.Security.PUT;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.TimeFrame.DAY_TRADE;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.TimeFrame.SWING;
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

        public abstract static class Position {

            final String ticker;
            final BuySell buySell;
            final double entryPrice;
            final double orderSize;
            TimeFrame timeFrame;
            String reason;

            Position(String ticker, BuySell buySell, double entryPrice,
                     double orderSize, TimeFrame timeFrame, String reason) {
                this.ticker     = ticker;
                this.buySell    = buySell;
                this.entryPrice = entryPrice;
                this.orderSize  = orderSize;
                this.timeFrame  = timeFrame;
                this.reason     = reason;
            }

            Position(String ticker, BuySell buySell, double entryPrice,
                     double orderSize, String reason) {
                this.ticker     = ticker;
                this.buySell    = buySell;
                this.entryPrice = entryPrice;
                this.orderSize  = orderSize;
                this.reason     = reason;
            }

            Position(String ticker, BuySell buySell, double entryPrice,
                     double orderSize, TimeFrame timeFrame) {
                this.ticker     = ticker;
                this.buySell    = buySell;
                this.entryPrice = entryPrice;
                this.orderSize  = orderSize;
                this.timeFrame  = timeFrame;
                this.reason     = reason;
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

        }

        public final class OptionPosition extends Position {

            final double targetPrice;
            final int[] expiration;

            OptionPosition(String ticker, BuySell buySell, double entryPrice,
                           double targetPrice, int[] expiration, double orderSize,
                           TimeFrame timeFrame, String reason) {
                super(ticker, buySell, entryPrice, orderSize, timeFrame, reason);
                this.targetPrice = targetPrice;
                this.expiration = expiration;
            }

            OptionPosition(String ticker, BuySell buySell, double entryPrice,
                           double targetPrice, int[] expiration, double orderSize,
                           String reason) {
                super(ticker, buySell, entryPrice, orderSize, reason);
                this.targetPrice = targetPrice;
                this.expiration = expiration;
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

        final ConcurrentHashMap<Long, ArrayList<Position>> userPositionMap;

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

            // in/out
            BuySell bySl;
            String bs = args.remove(0);
            if (bs.matches("i+n*")) {
                bySl = BUY;
            } else if (bs.matches("o+u*t*")){
                bySl = SELL;
            } else {
                event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                return;
            }

            // [$]TICKER
            String tkr = args.remove(0).replaceAll("[^A-Za-z]", "");
            if (tkr.isEmpty()) {
                //No ticker, do something about it TODO
                event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                return;
            }

            // [$]targetPrice
            String targetPrice = args.remove(0);
            Security security = null;
            double targetDouble;
            if (targetPrice.matches("^\\$?\\d*(\\.\\d*)?(c+(a*l*l*)|p+(u*t*))?$")) {
                if (targetPrice.matches("p+u*t*")) {
                    security = PUT;
                } else if (targetPrice.matches("c+a*l*l*")) {
                    security = CALL;
                }
                targetPrice = targetPrice.replaceAll("[^0-9.]", "");//Clear non-digits

                try { //Attempt to parse the price Double from the string
                    targetDouble = Double.parseDouble(targetPrice);
                } catch (NumberFormatException e) {
                    //TODO could not parse a double
                    event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                    System.out.println(targetPrice);
                    return;
                }
            } else {
                //Something not good TODO
                event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                return;
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
                    return;
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
                    return;
                }
                for (int i = 0; i < dts.length; i++) {
                    try {
                        exp[i] = Integer.parseInt(dts[i]);
                    } catch (NumberFormatException e) {
                        //Something not good TODO
                        event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                        return;
                    }
                }
            }

            // [at|@][$]entryPrice
            String at = args.remove(0);
            // 1st check if it is connected or not
            if (at.matches("^(a+(t*))|@+$")) { at = args.remove(0); }
            // Then check the price
            double entryPrice;
            if (at.matches(".\\$*\\d*(\\.\\d*)?")) {
                at = at.replaceAll("[^0-9.]", "");//Clear non-digits
                try { //Attempt to parse the price Double from the string
                    entryPrice = Double.parseDouble(at);
                } catch (NumberFormatException e) {
                    //TODO could not parse a double
                    event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                    System.out.println(at);
                    return;
                }
            } else {
                event.getMessage().addReaction(Unicode.CROSS_MARK.val).queue();
                System.out.println(at);
                return;
            }

            // [orderSize]
            double size = 1;
            String oSze = args.remove(0);
            String reason;
            try {
                size = Double.parseDouble(oSze);
                if (args.isEmpty()) {
                    reason = "";
                } else {
                    reason = args.remove(0);
                }
            } catch (NumberFormatException e) {
                // [description]
                reason = oSze;
            }

            TimeFrame tf = null;
            if (reason.matches(".*s+w*i*n*g*.*")) {
                tf = SWING;
            } else if (reason.matches(".*d+((t+)|(ay(trade)?)).*")) {
                tf = DAY_TRADE;
            }

            OptionPosition position = new OptionPosition(tkr, bySl, entryPrice,
                                                         targetDouble, exp, size,
                                                         tf, reason);

            event.getMessage().addReaction(Unicode.HEAVY_CHECK.val).queue();

        }

        @Override
        public boolean dead() {
            return deathRow;
        }

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

        ArrayList<PositionTracker> trackers = bot.getPassives(PositionTracker.class);
        PositionTracker chTracker = null;
        for (PositionTracker tracker : trackers) {
            if (tracker.channelID == event.getTextChannel().getIdLong()) {
                chTracker = tracker;
            }
        }

        Action action = parseAction(args[1]);

        switch (action) {
            case INIT: //ptc init [name]
                if (chTracker == null) {
                    String name;
                    if (args.length == 3) {
                        name = args[2];
                    } else {
                        name = event.getTextChannel().getName() + " Position Tracker";
                    }
                    bot.addPassive(new PositionTracker(event.getTextChannel(), name));
                }

                event.reply("Your PositionTracker has been initiated! *May the Tendies be with you.*");
                event.getMessage().addReaction(Unicode.HEAVY_CHECK.val).queue();
                return;
            case CLOSE:
                trackers.remove(chTracker);
                break;
            case SEE:
                break;
            default:

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
            case "seeAll":
            case "tendies":
            case "porties":
            default: return Action.SEE;
        }
    }


    @Override
    public MessageEmbed getEmbedHelp() {
        return super.getEmbedHelp();//TODO
    }
}
