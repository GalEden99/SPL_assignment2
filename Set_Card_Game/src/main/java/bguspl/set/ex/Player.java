package bguspl.set.ex;

import java.security.spec.EncodedKeySpec;
import java.util.ArrayDeque;
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
        if (!human) createArtificialIntelligence();

        ////////////////////// for testing ///////////////////////
        System.out.println("Enter the main loop (run()) of " + Thread.currentThread().getName());

        while (!terminate) {
            // TODO implement main player loop
            while (!queueOfKeyPresses.isEmpty()){

                int currSlot = queueOfKeyPresses.remove();
                
                System.out.println("Player.run(): is the queue of tokens contain " + currSlot + "? " + table.containsToken(id, currSlot));

                if (table.containsToken(id, currSlot)){ //fix bug
                    table.removeToken(id, currSlot);
                    
                } else {

                    int amountOfTokens = table.countTokens(id);

                    if (amountOfTokens<3){
                        table.placeToken(id, currSlot);
                    }
                    
                    if (amountOfTokens==3){

                        // block the player from pressing on a key
                        keyPressedOpen = false;

                        //send the set to the dealer for checking
                        int[] currSetOfCards = convertToSetOfCard(id); //HEREEEEEEE

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
                try {
                    wait();
                } catch (InterruptedException ignored) {}
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

         // should lock the option to press a key while the dealer is placing\removing cards 
        synchronized(dealer.lock){
            
            while (queueOfKeyPresses.size()>=3 && !keyPressedOpen){
                try{
                    playerThread.wait();
                } catch (InterruptedException e) { 
                }
            }
    
            if (queueOfKeyPresses.size()<3){
                queueOfKeyPresses.add(slot);
    
                //////////////////////////// FOR TESTING ////////////////////////////
                System.out.println("Player.keyPressed: Player " + id + " pressed " + slot);
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
        try{
            System.out.println("Player.point: Player " + id + " has a point and is frozen");
            Thread.sleep(env.config.pointFreezeMillis);
        } catch (InterruptedException e) {
        }

        System.out.println("Player.point: Player " + id + " is unfrozen");
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
            System.out.println("Player.penalty: Player " + id + " has been penalized and is frozen");
            Thread.sleep(env.config.penaltyFreezeMillis);
        } catch (InterruptedException e) {
        }

        System.out.println("Player.penalty: Player " + id + " is unfrozen");
        queueOfKeyPresses.clear();
        keyPressedOpen = true;

    }

    public int getScore() {
        return score;
    }


    //////////////////////// METHODS ADDED ////////////////////////

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

    //geter for the thread of the player
    public Thread getPlayerThread(){
        return playerThread;
    }

    // geter for the player id
    public String getId(){
        return "Player " + id;
    }

    // setter for the answer from the dealer
    public void setAnsFromCheckSet(int ans){
        ansFromCheckSet = ans;
    }

}
