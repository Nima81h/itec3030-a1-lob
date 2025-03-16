package ca.yorku.cmg.lob.exchange;

import java.util.ArrayList;

import ca.yorku.cmg.lob.orderbook.Ask;
import ca.yorku.cmg.lob.orderbook.Bid;
import ca.yorku.cmg.lob.orderbook.Orderbook;
import ca.yorku.cmg.lob.tradestandards.IOrder;
import ca.yorku.cmg.lob.tradestandards.ITrade;
import ca.yorku.cmg.lob.tradestandards.OrderOutcome;

/**
 * Represents a stock exchange that manages securities, accounts, orders, and trades.
 */
public class Exchange {

    Orderbook book;
    SecurityList securities = new SecurityList();
    AccountsList accounts = new AccountsList();
    ArrayList<ITrade> tradesLog = new ArrayList<>();

    long totalFees = 0;

    /**
     * Default constructor for the Exchange class.
     */
    public Exchange() {
        book = new Orderbook();
    }

    /**
     * Validates an order to ensure it complies with exchange rules. Checks if trader and security are supported by the exchange, and that the trader has enough balance in the exchange.
     *
     * @param o the {@linkplain ca.yorku.cmg.lob.tradestandards.IOrder}-implementing object to be validated
     * @return {@code true} if the order is valid, {@code false} otherwise
     */
    public boolean validateOrder(IOrder o) {
        // Check if the security associated with the order exists in the list of securities
        if (securities.getSecurity(o.getTicker()) == null) { // ✅ Fix: Correct null check
            System.err.println("Order validation: ticker " + o.getTicker() + " not supported.");
            return false;
        }

        // Check if the trader exists
        if (accounts.getAccount(o.getTraderID()) == null) { // ✅ Fix: Used `getTraderID()` instead of `getID()`
            System.err.println("Order validation: trader with ID " + o.getTraderID() + " not registered with the exchange.");
            return false;
        }

        int pos = accounts.getAccount(o.getTraderID()).getPosition(o.getTicker());
        long bal = accounts.getAccount(o.getTraderID()).getBalance();

        if ((o instanceof Ask) && (pos < o.getQuantity())) {
            System.err.println("Order validation: seller with ID " + o.getTraderID() +
                " does not have enough shares of " + o.getTicker() +
                ": has " + pos + " and tries to sell " + o.getQuantity());
            return false;
        }

        if ((o instanceof Bid) && (bal < o.getValue())) {
            System.err.println(String.format(
                "Order validation: buyer with ID %d does not have enough balance: has $%,.2f and tries to buy for $%,.2f",
                o.getTraderID(), bal / 100.0, o.getValue() / 100.0));
            return false;
        }

        return true;
    }

    /**
     * Processes and submits an order to the order book.
     *
     * @param o    The order to be processed
     * @param time The timestamp of the order
     */
    public void submitOrder(IOrder o, long time) {
        if (!validateOrder(o)) {
            return;
        }

        OrderOutcome oOutcome;

        // This is a bid for a security
        if (o instanceof Bid) {
            // Go to the asks half-book, check if there are matching asks (selling offers) and process them
            oOutcome = book.getAskbook().processOrder(o, time); // ✅ Fix: Used correct method name
            if (oOutcome.getUnfulfilledOrder().getQuantity() > 0) {
                book.getBidBook().addOrder(oOutcome.getUnfulfilledOrder()); // ✅ Fix: Correct capitalization
            }
        } else { // Order is an ask
            // Go to the bids half-book and check if there are matching bids (buying offers) and process them
            oOutcome = book.getBidBook().processOrder(o, time);
            // If the quantity of the unfulfilled order in the outcome is not zero
            if (oOutcome.getUnfulfilledOrder().getQuantity() > 0) {
                book.getAskbook().addOrder(oOutcome.getUnfulfilledOrder()); // ✅ Fix: Used `getAskbook()`
            }
        }

        if (oOutcome.getResultingTrades() != null) {
            tradesLog.addAll(oOutcome.getResultingTrades());
        } else {
            return;
        }

        // Process the resulting trades
        for (ITrade t : oOutcome.getResultingTrades()) {
            long buyerFee = t.getBuyerFee(); // ✅ Fix: Corrected buyer fee retrieval
            long sellerFee = t.getSellerFee(); // ✅ Fix: Corrected seller fee retrieval

            accounts.getAccount(t.getBuyerID()).applyFee(buyerFee);
            accounts.getAccount(t.getBuyerID()).applyTradePayment(-t.getTotalValue());
            accounts.getAccount(t.getBuyerID()).addPosition(t.getTicker(), t.getQuantity());

            accounts.getAccount(t.getSellerID()).applyFee(sellerFee);
            accounts.getAccount(t.getSellerID()).applyTradePayment(t.getTotalValue());
            accounts.getAccount(t.getSellerID()).removePosition(t.getTicker(), t.getQuantity());

            this.totalFees += buyerFee + sellerFee;
        }
    }
}
