package ca.yorku.cmg.lob.exchange;

import ca.yorku.cmg.lob.stockexchange.event.NewsBoard;
import ca.yorku.cmg.lob.stockexchange.tradingagent.TradingAgent;
import ca.yorku.cmg.lob.stockexchange.tradingagent.AbstractTradingAgentFactory;
import ca.yorku.cmg.lob.stockexchange.tradingagent.ITradingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * The Exchange class simulates a stock exchange.
 */
public class Exchange {
    private List<TradingAgent> traders;
    private NewsBoard newsBoard;

    public Exchange() {
        this.traders = new ArrayList<>();
        this.newsBoard = new NewsBoard();
    }

    /**
     * Initializes traders from a configuration file.
     */
    public void initializeTraders() {
        // Example of creating traders using the Abstract Factory pattern
        traders.add(AbstractTradingAgentFactory.createTradingAgent("Retail", "Conservative", newsBoard));
        traders.add(AbstractTradingAgentFactory.createTradingAgent("Institutional", "Aggressive", newsBoard));
    }

    /**
     * Runs the simulation by processing stock market events.
     */
    public void runSimulation() {
        System.out.println("Running stock exchange simulation...");
        newsBoard.runEventsList(); // This triggers event notifications to registered traders
    }

    /**
     * Displays trader information.
     */
    public void displayTraders() {
        for (TradingAgent trader : traders) {
            System.out.println(trader);
        }
    }

    public static void main(String[] args) {
        Exchange exchange = new Exchange();
        exchange.initializeTraders();
        exchange.runSimulation();
        exchange.displayTraders();
    }
}
