package bguspl.set.ex;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    //////////////////////// FIELDS ADDED ////////////////////////

    private Dealer dealer; //the dealer object

    //private boolean isLegal = true; //false if the player has made an illegal move (i.e. the player has pressed a key that is not allowed)

    private Queue<Integer> queueOfKeyPresses = new LinkedList<>(); //the queue of key presses

    private int ansFromCheckSet = 0; //-1 if the set is not legal, 0 if there is no set to check, 1 if the set is legal

    private boolean keyPressedOpen = true; //true if the player can press on a key, false if the player cannot press on a key

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");

        System.out.println("Player.run(): " + Thread.currentThread().getName() + " is " + (human ? "human" : "computer") );

        if (!human) createArtificialIntelligence();

        ////////////////////// for testing ///////////////////////
        System.out.println("Enter the main loop (run()) of " + Thread.currentThread().getName());

        while (!terminate) {
            // TODO implement main player loop
            while (!queueOfKeyPresses.isEmpty()){

                int currSlot = queueOfKeyPresses.remove();

                // notify the ai thread that the key press has been processed
                if (!human) aiThread.interrupt();
                
                System.out.println("Player.run(): is the queue of tokens contain " + currSlot + "? " + table.containsToken(id, currSlot));

                if (table.containsToken(id, currSlot)){ 
                    table.removeToken(id, currSlot);
                    
                } else {

                    if (table.countTokens(id)<3){
                        table.placeToken(id, currSlot);
                    }
                    
                    if (table.countTokens(id)==3){

                        // block the player from pressing on a key and *wait* for the dealer to check if the set is legal
                        keyPressedOpen = false;

                        //send the set to the dealer for checking
                        int[] currSetOfCards = convertToSetOfCard(id); 

                        dealer.queueOfPlayersId.add(id);
                        dealer.queueOfSets.add(currSetOfCards);
                        
                        dealer.getThread().interrupt(); //notify the dealer that the player has placed 3 tokens

                        synchronized(this){
                            try{
                                wait(); //wait for the dealer to check if the set is legal (blocking the player thread from another key press)
                            } catch (InterruptedException e) {
                            }
                        }

                        System.out.println("player " + id + ": I AM AWAKE");

                        if (ansFromCheckSet == -1){
                            //the set is not legal
                            penalty();
                            ansFromCheckSet = 0;
                        } else if (ansFromCheckSet == 1){
                            //the set is legal
                            point();
                            ansFromCheckSet = 0;
                        } 

                    }

                }
            } 
        }

        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }


    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                int slot = (int) (Math.random() * 12);
                //keyPressed(slot);
                synchronized(this){
                    while (queueOfKeyPresses.size() == 3){
                        try{
                            wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    
                    keyPressed(slot);
                    notify(); // ???

                }

                // try {
                //     wait();
                // } catch (InterruptedException ignored) {}
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement

        System.out.println("Player.keyPressed: Thread " + Thread.currentThread().getName() + " is " + (human ? "human" : "computer" + "\n\tslot is: " + slot) );

         // should lock the option to press a key while the dealer is placing\removing cards 
        synchronized(dealer.lock){

            System.out.println( "\n\tamountOfTokens: " + table.countTokens(id));
    
            if (table.countTokens(id)<=3 && keyPressedOpen){
                queueOfKeyPresses.add(slot);
    
                //////////////////////////// FOR TESTING ////////////////////////////
                System.out.println("Player.keyPressed: Player " + id + " pressed " + slot + "\n\tamountOfTokens: " + table.countTokens(id));
            } 
        }
   
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        score++;

        // update the maxScore in the dealer
        if (score > dealer.maxScore){
            dealer.maxScore = score;
        } 

        try{
            System.out.println("Player.penalty: Thread " + Thread.currentThread().getName() + " has been penalized and is frozen");
            Thread.sleep(env.config.pointFreezeMillis);
        } catch (InterruptedException e) {
        }

        System.out.println("Player.penalty: Thread " + Thread.currentThread().getName() + " has been penalized and is frozen");
        queueOfKeyPresses.clear();
        keyPressedOpen = true;

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement 
        try{
            System.out.println("Player.penalty: Thread " + Thread.currentThread().getName() + " has been penalized and is frozen");
            Thread.sleep(env.config.penaltyFreezeMillis);
        } catch (InterruptedException e) {
        }

        System.out.println("Player.penalty: Thread " + Thread.currentThread().getName() + " has been penalized and is frozen");
        queueOfKeyPresses.clear();
        keyPressedOpen = true;

    }

    public int getScore() {
        return score;
    }


    //////////////////////// METHODS ADDED ////////////////////////

    // setter for the answer from the dealer
    public void setDealer(Dealer dealer) {
        this.dealer = dealer;
    }

    public int[] convertToSetOfCard(int playerId){
        int[] setOfCards = new int[3];

        int[] listOfTokensSlots = table.getTokensSlots(playerId);

        int i = 0;
        for (int currSlot: listOfTokensSlots) {
            setOfCards[i] = table.slotToCard[currSlot];
            i++;
        }
        return setOfCards;
    }

    //getter for the thread of the player
    public Thread getPlayerThread(){
        return playerThread;
    }

    // getter for the player id
    public int getId(){
        return id;
    }

    // setter for the answer from the dealer
    public void setAnsFromCheckSet(int ans){
        ansFromCheckSet = ans;
    }

}
