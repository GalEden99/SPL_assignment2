package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected volatile Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected volatile Integer[] cardToSlot; // slot per card (if any)


    //////////////////////// FIELDS ADDED ////////////////////////

    //the tokens that the player has on the table (queueOfTokens[player][slot])
    protected volatile boolean[][] playerTokens; // tokens per player per slot (if any)


    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        playerTokens = new boolean[env.config.players][env.config.tableSize];
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
        playerTokens = new boolean[env.config.players][env.config.tableSize];
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;

        // TODO implement
        env.ui.placeCard(card, slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // TODO implement
        int card = slotToCard[slot];
        cardToSlot[card] = null; 
        slotToCard[slot] = null; 
        for (int i = 0; i< env.config.players; i++){
            removeToken(i, slot);
        }
        env.ui.removeCard(slot);
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        // TODO implement
        
        if (slotToCard[slot] != null){

            ////////////////////// for testing ///////////////////////
            System.out.println("table.placeToken: " + player + ": " + slot);
            playerTokens[player][slot] = true;
            env.ui.placeToken(player, slot);
        }
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        // TODO implement 
        // implement try and catch 
        if (slotToCard[slot] != null){
            
            ////////////////////// for testing ///////////////////////
            System.out.println("            table.removeToken: " + player + ": " + slot);
            playerTokens[player][slot] = false;
            env.ui.removeToken(player, slot);
            return true;
        }
        return false;
    }


    //////////////////////// METHODS ADDED ////////////////////////

    // contains method for the queue of tokens
    public boolean containsToken(int player, int slot){
        return playerTokens[player][slot];
    }

    // returns the number of tokens on the table for a player
    public int countTokens(int player){
        int tokens = 0;
        for (int i = 0; i < env.config.tableSize; i++){
            if (playerTokens[player][i]){
                tokens++;
            }
        }
        return tokens;
    }
    
    // returns the slots that the player has tokens on
    public int[] getTokensSlots(int player){
        int[] slots = new int[3];
        int index = 0;
        for (int i = 0; i < env.config.tableSize; i++){
            if (playerTokens[player][i]){
                slots[index] = i;
                index++;
            }
        }
        return slots;
    }
}
