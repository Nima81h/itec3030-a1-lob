package ca.yorku.cmg.lob.exchange;

import java.util.ArrayList;

import ca.yorku.cmg.lob.orderbook.Ask;
//...

/**
 * Represents a stock exchange that manages securities, accounts, orders, and trades.
 */
public class Exchange {

    Orderbook book;
    SecurityList securities = new SecurityList();
    AccountsList accounts = new AccountsList();
    ArrayList tradesLog = new ArrayList();

    long totalFees = 0;

    /**
     * Default constructor for the Exchange class.
     */
    public Exchange(){
        book = new Orderbook();
    }

    /**
     * Validates an order to ensure it complies with exchange rules. Checks if trader and security are supported by the exchange, and that the trader has enough balance of the exchange.
     *
     * @param o the {@linkplain ca.yorku.cmg.lob.tradestandards.IOrder}-implementing object to be validated
     * @return {@code true} if the order is valid, {@code false} otherwise
     */
    public boolean validateOrder(IOrder o) {
        // Does ticker exist? See if the security associated with the order exists in the list of securities
        if (securities.getSecurity(o.getTicker()== null){
            System.err.println("Order validation: ticker " + o.getTicker() + " not supported.");
            return (false);
        }
//Does the trader exist? Check to see if the trader exists
        if (accounts.getAccount(o.getTraderID())==null){
            System.err.println("Order validation: trader with ID " + o.getID() + " not registered with the exchange.");
            return (false);
        }
        int pos = accounts.getAccount(o.getTraderID()).getPosition(o.getTicker());
        long bal =accounts.getAccount(o.getTraderID()).getBalance();
        if ((o instanceof Ask) && (pos < o.getQuantity())) {
            System.err.println("Order validation: seller with ID " +o.getID() + " not enough shares
                    of " + o.getTicker() + ": has " + pos + " and tries to sell " +
            o.getQuantity());
            return (false);
        }
        if ((o instanceof Bid) && (bal < o.getValue())) {
            System.err.println(
                    String.format("Order validation: buyer with ID %d does not have enough balance: has $%,.2f and tries to buy for $%,.2f",
                            o.getID(), bal/100.0,o.getValue()/100.0));
            return (false);
        }
        return (true);
    }
    public void submitOrder(IOrder o, long time) {
        if (!validateOrder(o)){
            return;
        }

        OrderOutcome oOutcome;

        //This is a bid for a security
        if (o instanceof Bid) {// Order is a bid
            //Go to the asks half-book, see if there are matching asks (selling offers) and process them
            oOutcome =book.getAskbook().processOrder(o, time);
            if(oOutcome.getunfulfilledorder().getQuantity>0){
                book.getBidBook().addOrder(oOutcome.getUnfulfilledOrder());
            }
        } else { //order is an ask
            //Go to the bids half-book and see if there are matching bids (buying offers) and process them
            oOutcome =book.getBidBook().processOrder(o, time);
            //If the quanity of the unfulfilled order in the outcome is not zero
            if (oOutcome.getUnfulfilledOrder().getQuantity() > 0) {
                book.getBidBook().addOrder(oOutcome.getUnfulfilledOrder())
            }
        }
        if (oOutcome.getResultingTrades() != null) {
            tradesLog.addAll(oOutcome.getResultingTrades());
        } else {
            return;
        }
        for (ITrade t:oOutcome.getResultingTrades()) {
            t.getBuyerFee();
            accounts.getAccount(t.getBuyerID()).applyFee(buyerFee);
            accounts.getAccount(t.getBuyerID()).applyTradePayment(-t.getTotalValue());
            accounts.getAccount(t.getBuyerID()).addPosition(t.getTicker(), t.getQuantity());
            t.getSellerFee();
            accounts.getAccount(t.getSellerID()).applyFee(sellerFee);
            accounts.getAccount(t.getSellerID()).applyTradePayment(-t.getTotalValue());
            accounts.getAccount(t.getSellerID()).removePosition(t.getTicker(), t.getQuantity());
            this.totalFees += t.getBuyerFee() + t.getSellerFee();
        }
    }
