package com.ampro.weebot.commands.stonks;

import com.ampro.weebot.bot.Weebot;
import com.ampro.weebot.commands.Command;
import com.ampro.weebot.commands.IPassive;
import com.ampro.weebot.commands.properties.Restriction;
import com.ampro.weebot.listener.events.BetterMessageEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import static com.ampro.weebot.commands.stonks.PositionTrackerCommand.PositionTracker.Security.CALL;
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

        abstract static class Position {

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
        final class StockPostion extends Position {

            public StockPostion(String ticker, BuySell buySell, float entryPrice,
                                float orderSize, TimeFrame timeFrame, String reason) {
                super(ticker, buySell, entryPrice, orderSize, timeFrame, reason);

            }

        }

        final class OptionPosition extends Position {

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


        final Restriction modRestriction = new Restriction();

        Security defaultType = CALL;

        final ConcurrentHashMap<Long, ArrayList<Position>> userPositionMap
                = new ConcurrentHashMap<>();

        /** Whether this {@link IPassive} is set to be destroyed on the next scan */
        boolean deathRow = false;


        public PositionTracker(Guild guild, String name, Date start) {

        }



        /**
         * TODO
         *
         * @param bot The weebot who called
         * @param event The event to receive.
         */
        @Override
        public void accept(Weebot bot, BetterMessageEvent event) {

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
