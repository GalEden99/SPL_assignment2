package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    protected Queue<int[]> queueOfSets = new ConcurrentLinkedQueue<>(); //queue of sets

    protected Queue<Integer> queueOfPlayersId = new ConcurrentLinkedQueue<>(); //queue of sets and player id

    // The thread representing the dealer
    private Thread dealerThread;

    protected Object lock = new Object(); //lock object for the dealer threads

    protected int maxScore;

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

        dealerThread = Thread.currentThread();

        // Set the dealer and player threads for each player
        for (int i = 0; i < players.length; i++) {
            players[i].setDealer(this);
            Thread playerThread = new Thread(players[i], "player " + players[i].getId());
            playerThread.start();
        }

        while (!shouldFinish()) {
            placeCardsOnTable();
            
            //updateing the reshuffle time before the timer loop
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis; 

            timerLoop();

            //updateTimerDisplay(false);
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

            // checks if there is a set to check in the queue
            checkQueueOfSets();

            // checks if there is a legal set on the table
            if (env.util.findSets(deck, 1).size() == 0){
                terminate();
            }

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
        synchronized(lock){
            for (int i = 0; i < cards.size(); i++) {
                int card = cards.get(i);
                int slot = table.cardToSlot[card];
                table.removeCard(slot);
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        synchronized(lock){
          
            if (deck.size() != 0){
            
                //finding an empty random slot
                List<Integer> emptySlots = findEmptySlot();

                for (int i = emptySlots.size()-1; i >= 0; i--) {
                    Collections.shuffle(emptySlots);
                    int slot = emptySlots.remove(i);
                    
                    Collections.shuffle(deck);
                    
                    if (deck.size() != 0){ // if the deck is not empty
                        int card = deck.remove(0);
                        table.placeCard(card,slot);

                        System.out.println("card: " + card + " slot: " + slot);
                    }

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
            //System.out.println("Thread " + Thread.currentThread().getName() + " is sleeping for tableDelayMillis");
            Thread.sleep(env.config.tableDelayMillis);
                } catch (InterruptedException exception) {
                    System.out.println("Thread " + Thread.currentThread().getName() + " interrupted.");
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
        synchronized(lock){
            env.ui.removeTokens();
            for (int i=0; i<env.config.tableSize; i++){  
                table.removeCard(i);
            }
        }

    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        Queue<Player> winnersQueue = new LinkedList<Player>();

        for (int i = 0; i < players.length; i++) {
            if (players[i].getScore() == maxScore){
                winnersQueue.add(players[i]);
            }
        }

        int[] winnerArray = new int[winnersQueue.size()];
        for (int i = 0; i < winnerArray.length; i++) {
            winnerArray[i] = winnersQueue.remove().getId();
        }

        env.ui.announceWinner(winnerArray);
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

    //checks if the cards are a set
    public boolean checkSet(int playerId, int[] cards){
        boolean isSet = env.util.testSet(cards);
        System.out.println("Dealer: checkSet, isSet: " + isSet);

        if (isSet){
            removeCardsFromTable(Arrays.stream(cards).boxed().collect(Collectors.toList()));
            players[playerId].setAnsFromCheckSet(1);
            
            synchronized(players[playerId].getPlayerThread()){
                players[playerId].getPlayerThread().interrupt();
            }

            updateTimerDisplayForPlayer(playerId, isSet);
            
            // when a set is found the reshuffleTime is updated
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;

            //updateTimerDisplay(isSet);

        } else {
            players[playerId].setAnsFromCheckSet(-1);

            synchronized(players[playerId].getPlayerThread()){
                System.out.println("Dealer: checkSet, notify the player");
                players[playerId].getPlayerThread().interrupt();
            }
            
            updateTimerDisplayForPlayer(playerId, isSet);

        }
        //notify();
        return isSet;
    }

    //checks if there is a set to check in the queue
    public void checkQueueOfSets(){
         if (!queueOfSets.isEmpty() && !queueOfPlayersId.isEmpty()){
            System.out.println("Dealer: queueOfSets");
            int playerID = queueOfPlayersId.remove();
            int[] set = queueOfSets.remove();
            System.out.println("set: " + table.cardToSlot[set[0]] + " " + table.cardToSlot[set[1]] + " " + table.cardToSlot[set[2]]);
            checkSet(playerID, set);
            }
    }

    // getter for the dealer thread
    public Thread getThread(){
        return dealerThread;
    }

    
    private void updateTimerDisplayForPlayer(int playerId, boolean isSet) {

        if (isSet){ // if the player had a legal set
            long pointFreezeTimer = System.currentTimeMillis()+env.config.pointFreezeMillis;
            while (System.currentTimeMillis() <= pointFreezeTimer){
                env.ui.setFreeze(playerId, pointFreezeTimer-System.currentTimeMillis()+1000); // show the player in red for env.config.pointFreezeMillis
                updateTimerDisplay(true);
            }
            env.ui.setFreeze(playerId, pointFreezeTimer-System.currentTimeMillis()); // show the player in black

        } else { // if the player had an illegal set
            long penaltyFreezeTimer = System.currentTimeMillis()+env.config.penaltyFreezeMillis;
            while (System.currentTimeMillis() <= penaltyFreezeTimer){
                env.ui.setFreeze(playerId, penaltyFreezeTimer-System.currentTimeMillis()+1000);  // show the player in red for env.config.penaltyFreezeMillis
                updateTimerDisplay(true);
            }
            env.ui.setFreeze(playerId, penaltyFreezeTimer-System.currentTimeMillis()); // show the player in black

        }
    }

}
