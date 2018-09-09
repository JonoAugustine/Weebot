package com.ampro.weebot.commands.stonks;

import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.commands.properties.Restriction;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import com.ampro.weebot.util.Unicode;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.BuySell.BUY;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.BuySell.SELL;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.Security.CALL;
import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.Security.PUT;
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
            final float entryPrice;
            final float orderSize;
            TimeFrame timeFrame;
            String reason;

            Position(String ticker, BuySell buySell, float entryPrice,
                            float orderSize, TimeFrame timeFrame, String reason) {
                this.ticker     = ticker;
                this.buySell    = buySell;
                this.entryPrice = entryPrice;
                this.orderSize  = orderSize;
                this.timeFrame  = timeFrame;
                this.reason     = reason;
            }

            Position(String ticker, BuySell buySell, float entryPrice,
                     float orderSize, String reason) {
                this.ticker     = ticker;
                this.buySell    = buySell;
                this.entryPrice = entryPrice;
                this.orderSize  = orderSize;
                this.reason     = reason;
            }

            Position(String ticker, BuySell buySell, float entryPrice,
                     float orderSize, TimeFrame timeFrame) {
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

            final float targetPrice;
            final int[] expiration;

            OptionPosition(String ticker, BuySell buySell, float entryPrice,
                           float targetPrice, int[] expiration, float orderSize,
                           TimeFrame timeFrame, String reason) {
                super(ticker, buySell, entryPrice, orderSize, timeFrame, reason);
                this.targetPrice = targetPrice;
                this.expiration = expiration;
            }

        }

        /** Restrictions of Tracker Modification */
        final Restriction modRestriction = new Restriction();

        /** The TextChannel to track within */
        TextChannel channel;

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
            this.channel = channel;
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

            // in/out
            BuySell buySell;
            if (args.remove(0).matches("i+n*")) {
                buySell = BUY;
            } else {
                buySell = SELL;
            }

            // [$]TICKER
            String ticker = args.remove(0).replaceAll("[^a-z]", "");
            if (ticker.isEmpty()) {
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
                targetPrice = targetPrice.replaceAll("[^0-9]", "");//Clear non-digits

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

            }

            // [at|@][$]entryPrice
            // [orderSize]
            // [description]

            event.getMessage().addReaction(Unicode.HEAVY_CHECK.val).queue();

        }

        @Override
        public boolean dead() {
            return deathRow;
        }

    }

    /** Build a Command with each variable assigned. */
    protected PositionTrackerCommand() {
        super("PositionTracker", new String[]{"ptc", "position", "trades"},
              "Track stock and option positions.",
              "in/out/buy/sell [$]TICKER [$]targetPrice type expiration/date [@]entryPrice"
                      + "[OrderSize] [day|Swing] [reason]",
              true, false, 0, false, false);
        this.userPermissions = new Permission[] {ADMINISTRATOR, MANAGE_CHANNEL};
    }

    private enum Action { INIT, CLOSE, SEE_ALL, SEE_AT}

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

    }



}
