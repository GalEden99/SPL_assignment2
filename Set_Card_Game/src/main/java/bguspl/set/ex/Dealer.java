package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.lang.Math;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    //////////////////////// FIELDS ADDED ////////////////////////

    private Object lockCardsAndTokens = new Object(); //lock object for the dealer and players threads

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        while (!shouldFinish()) {
            placeCardsOnTable();
            
            //updateing the reshuffle time before the timer loop
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis; 

            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
       
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(true);
            //removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable(List<Integer> cards) { // we changed the method signature to get a list of cards to remove
        // TODO implement
        synchronized(lockCardsAndTokens){
            for (int i = 0; i < cards.size(); i++) {
                int card = cards.get(i);
                int slot = table.cardToSlot[card];
                env.ui.removeTokens(slot);
                table.removeCard(slot);
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        synchronized(lockCardsAndTokens){
            if (deck.size() != 0){
                //finding an empty random slot
                List<Integer> emptySlots = findEmptySlot();

                for (int i = 0; i < emptySlots.size(); i++) {

                    int slot = emptySlots.get((int) (Math.random() * emptySlots.size()));
                    int slotIndex = emptySlots.indexOf(slot);
                    
                    Collections.shuffle(deck);
                    int card = deck.get(0);
                    table.placeCard(card,slot);
                    deck.remove(0);

                    //removing the slot from the list of empty slots
                    emptySlots.remove(slotIndex);
                }   
            }
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        try {
            Thread.sleep(env.config.tableDelayMillis);
                } catch (InterruptedException exception) {
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if (reset){
            Boolean warning = env.config.turnTimeoutWarningMillis >= reshuffleTime-System.currentTimeMillis();
            env.ui.setCountdown(Math.max(0, reshuffleTime-System.currentTimeMillis()), warning);
        }

    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
        synchronized(lockCardsAndTokens){
            env.ui.removeTokens();
            for (int i=0; i<env.config.tableSize; i++){
                if (table.slotToCard[i] != null){
                    int card = table.slotToCard[i];
                    table.slotToCard[i] = null;
                    table.cardToSlot[i] = null;
                    deck.add(card);

                }
            }
        }

    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
    }

    ///////////////////////////////// new methodes /////////////////////////////////

    //finds an empty slot on the table
    private List<Integer> findEmptySlot(){
        List<Integer> emptySlots = new ArrayList<Integer>();
        for (int i = 0; i < env.config.tableSize; i++) {
            if (table.slotToCard[i] == null){
                emptySlots.add(i);
            }
        }
        return emptySlots;
    }

    //geter for the lockCardsAndTokens object
    public Object getLockCardsAndTokens(){
        return lockCardsAndTokens;
    }
}
