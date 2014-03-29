package com.example.digitalDeck;

import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;

//TODO: group puts to make more efficient
public class EuchreGame extends Game{

    private static final String[] vals = {"9", "T", "J", "Q", "K", "A"}; 
    private static final String[] suits = {"S", "C", "H", "D"};

    private int state; //An integer representation of the state of the game
    private int dealer;
    private int caller;
    private int playerTurn; //The player who's turn it is
    private int[] scores;
    private int[] tricksTaken;
    private String[] kitty;
    private String[] trick;
    private int trickIndex;
    private String topCard;
    private int trump; //The index in the suits array for trump
    private int lead; //The suit that was lead
    private Server server;

    /**EuchreGame constructor
     * @param host the player who created the game
     * @param title the name of the game
     * Creates a game by the same means as the game constructor,
     * but also initializes Euchre class variables such as dealer and
     * scores
     */
    public EuchreGame(String host, String title) {
        super(4, host, title);
        state = 0;
        dealer = 0;
        scores = new int[2];
        kitty = new String[3];
        trick = new String[4];
        tricksTaken = new int[2];
        scores[0] = 0;
        scores[1] = 0;
        server = (Server)YourDealApplication.delegate;
    }

    /**startRound
     * Shuffles the deck, deals out each players hand, the kitty,
     * and specifies the turned up card then forwards the information
     * to each player
     */
    public void startRound() {
        if (state != 0) return; //Stop from being called at the wrong time
        System.out.println("Starting round");
        String hands[][] = new String[4][5];
        kitty = new String[3];
        String[] deck = makeDeck();
        topCard = deal(deck, hands);
        Hashtable<Object, Hashtable<String, Object>> updates = new Hashtable<Object, Hashtable<String, Object>>();
        for (int i = 0; i < super.players.length; i++) {
        	Hashtable<String, Object> playerUpdate = new Hashtable<String, Object>();
        	playerUpdate.put("target", "player");
            playerUpdate.put("hand", hands[i]);
            updates.put(players[i], playerUpdate);
        }
        YourDealApplication.delegate.updateProperties(updates);
        state = 1;
        playerTurn = dealer + 1;
        trump = -1;
        Hashtable<String, Object> gameUpdate = new Hashtable<String, Object>();
        gameUpdate.put("target", "game");
        gameUpdate.put("topCard", topCard);
        server.updateGame(gameUpdate);
    }

    /**chooseTrump
     * The chooseTrump method has 2 states
     * State 1: Each player recieves a query on their respective turn to call on whether
     * or not they want the suit of the top card to be trump. If a call is made this way
     * the top card is picked up by the dealer and replaces another card in the dealer's
     * hand.
     * State 2: Each player recieves a query to choose trump so long as it is not the suit
     * that was turned up.
     * In either state the calling player may choose to go alone, nulling out their partner's hand
     */
    public void processCall1(String response) { //False for pass, true for pick it up
        if (state != 2) return; 
        if (response.equals("call")) { //If trump as called
            caller = playerTurn;
            trump = getSuit(topCard);
            Hashtable<String, Object> updates = new Hashtable<String, Object>();
            updates.put("target", "game");
            updates.put("trump", suits[trump]);
            server.updateGame(updates);
            state = 3;
        } else { //If trump was not called
            if (response.equals("farmers")) {
                //TODO add farmers logic
            }
            state = 1;
            if (playerTurn == dealer) state = 7;
            playerTurn++;
            if (playerTurn > 3) playerTurn -= 4;
            JSONObject empty = new JSONObject();
            process(empty);
        }
    }

    public void processDropCard(String toDrop) {
        String[] dealerHand = (String[]) super.players[dealer].get("hand");
        int index = getIndex(dealerHand, toDrop);
        if (index != -1) {
            dealerHand[index] = topCard;
        }
        Hashtable<String, Object> playerUpdate = new Hashtable<String, Object>();
        playerUpdate.put("target", "player");
        playerUpdate.put("hand", "dealerHand");
        Hashtable<Object, Hashtable<String, Object>> update = new Hashtable<Object, Hashtable<String, Object>>();
        update.put(players[dealer], playerUpdate);
        YourDealApplication.delegate.updateProperties(update);
        JSONObject empty = new JSONObject();
        state = 5;
        process(empty);
    }

    public void processLoner(boolean response) {
        if (response) {
            int partner = playerTurn + 2;
            if (partner > 3) partner -= 4;
            super.players[partner].put("hand", null);
        }
        playerTurn = dealer + 1;
        if (playerTurn > 3) playerTurn -= 4;
        tricksTaken = new int[2];
        trickIndex = 0;
        lead = -1;
        state = 9;
    }
        
    public void processCall2(String call) {
        if (state != 4) return;
        if (!call.equalsIgnoreCase("pass")) { //If the player called trump
            trump = getIndex(suits, call);
            caller = playerTurn;
            Hashtable<String, Object> updates = new Hashtable<String, Object>();
            updates.put("target", "game");
            updates.put("trump", suits[trump]);
            server.updateGame(updates);
            state = 5;
        } else {
            //TODO enable screw the dealer option where the dealer may not turn trump down
            state = 7;
            if (playerTurn == dealer) {
                dealer++;
                if (dealer > 3) dealer -= 4;
                state = 0;
            }
            playerTurn++;
            if (playerTurn > 3) playerTurn -= 4;
            JSONObject empty = new JSONObject();
            process(empty);
        }
    }

    /**playRound
     * Takes sends a query to the player who's turn it is with a
     * list of playable cards given the situation. Once the player
     * recieves a response the card is added to the trick and the
     * playerindex advances
     */
    public void processPlay(String play) {
        if (state != 10) return;
        if (super.players[playerTurn].get("hand") == null) {
            playerTurn++;
            if (playerTurn > 3) playerTurn -= 4;
            trick[trickIndex] = null;
            trickIndex++;
            return;
        }
        //Update hand
        String[] hand = (String[])super.players[playerTurn].get("hand");
        int index = 0;
        String[] newHand = new String[hand.length - 1];
        for (int i = 0; i < hand.length; i++) {
            if (!hand[i].equalsIgnoreCase(play)) {
                newHand[index] = hand[i];
                index++;
            }
        }
        Hashtable<String, Object> playerUpdate = new Hashtable<String, Object>();
        playerUpdate.put("target", "player");
        playerUpdate.put("hand", newHand);
        Hashtable<Object, Hashtable<String, Object>> update = new Hashtable<Object, Hashtable<String, Object>>();
        YourDealApplication.delegate.updateProperties(update);
        //Process lead card
        if (lead == -1) {
            lead = getSuit(play);
            if (isLeft(play)) lead = trump;
        }
        //Add to trick
        trick[trickIndex] = play;
        trickIndex++;
        playerTurn++;
        if (playerTurn > 3) playerTurn -= 4;
        state = 9;
        if (trickIndex > 3) {
            trickIndex = 0;
            state = 11;
        }
        Hashtable<String, Object> played = new Hashtable<String, Object>();
        played.put("cardPlayed", play);
        server.updateGame(played);
    }

    /**processTrick
     * Determines who the winner of the given trick was,
     * increments the trick count for the given team, changes
     * the player who's turn it is to the winner and continues
     * the game
     */
    public void processTrick() {
        if (state != 11) return;
        int winner = findWinner();
        tricksTaken[winner % 2]++;
        playerTurn = winner;
        if (tricksTaken[0] + tricksTaken[1] >= 5) {
            state = 12;
        } else {
            state = 9;
        }
        Hashtable<String, Object> updates = new Hashtable<String, Object>();
        updates.put("target", "game");
        updates.put("tricks", tricksTaken);
        server.updateGame(updates);
    }

    /**processDeal
     * Determines which team one the round of Euchre and
     * increments scores appropriately. If the game is over
     * the method transitions the program into a dead state
     * where information can be gathered but the game can no
     * longer be played.
     */
    public void processDeal() {
        if (state != 12) return;
        int winningTeam = 0;
        if (tricksTaken[1] >= 3) winningTeam = 1;
        //Determine scoring increment
        if (tricksTaken[winningTeam] == 5) {
            scores[winningTeam] += 2; //2 points for all 5 tricks
            if (wasLoner(winningTeam)) scores[winningTeam] += 2; //Extra 2 for loner
        } else if (caller % 2 != winningTeam) {
            scores[winningTeam] += 2; //Euch
        } else scores[winningTeam]++;
        dealer++;
        state = 0;
        if (scores[0] >= 10 || scores[1] >= 10) state = 13; //Game over
        Hashtable<String, Object> updates = new Hashtable<String, Object>();
        updates.put("target", "game");
        updates.put("scores", scores);
        server.updateGame(updates);
    }

    public void requestSignal() {
        Hashtable<String, Object> dict = new Hashtable<String, Object>();
        dict.put("target", "player");
        String key = "";
        switch (state) {
            case 1:
                //pick up/pass request
                key = "turn";
                break;
            case 3:
                //drop card request
                String[] cards = new String[6];
                String[] hand = (String[])super.players[dealer].get("hand");
                for (int i = 0; i < hand.length; i++) {
                    cards[i] = hand[i];
                }
                cards[5] = topCard;
                key = "drop";
                dict.put("validCards", cards);
                break;
            case 5:
                //Loner request
                key = "lone";
                break;
            case 7:
                //make call request
                int index = 0;
                String[] calls = new String[4];
                for (int i = 0; i < suits.length; i++) {
                    if (i != getSuit(topCard)) {
                        calls[index] = suits[i];
                        index++;
                    }
                }
                calls[3] = "pass"; //Disable for dealer if STD is on
                key = "call";
                dict.put("validCalls", calls);
                break;
            case 9:
                //play card request
                index = 0;
                hand = (String[])super.players[playerTurn].get("hand");
                String[] playable = new String[hand.length];
                for (int i = 0; i < playable.length; i++) {
                    if (canPlay(hand[i])) {
                        playable[index] = hand[i];
                    }
                }
                key = "play";
                dict.put("validCards", playable);
                break;
        }
        dict.put("action", key);
        super.players[playerTurn].updateProperties(dict);
        state++;
    }

    /**makeDeck
     * @return the newly made/shuffled deck
     * Makes a randomly sorted array of euchre cards
     * with no repeats containing every possible card in
     * euchre
     */
    public String[] makeDeck() {
        Random rand = new Random();
        String [] deck= new String[24];
        int val;
        int suit;
        for (int i = 0; i < 24; i++) {
            val = rand.nextInt(6);
            suit = rand.nextInt(4);
            String card = vals[val] + suits[suit];
            if (getIndex(deck, card) == -1) {
                deck[i] = card;
            } else {
                i--;
                continue;
            }
        }
        return deck;
    }

    public void requestRespones() {
        switch (state) {
            
        }
    }

    /**deal
     * @param deck a shuffled array of euchre cards
     * @param hands an empty multidimensional array for each players hand
     * @return the turned up card at the end of the deal
     * distributes 5 cards into each players hand array, adds a 3
     * card kitty for house rules such as farmers, and turns 1 card face
     * up for the determining of trump
     */
    private String deal(String[] deck, String[][] hands) {
        int deckPtr = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                hands[i][j] = deck[deckPtr];
                deckPtr++;
            }
        }
        String topCard = deck[deckPtr];
        deckPtr++;
        for (int i = 0; i < 3; i++) {
            kitty[i] = deck[deckPtr];
            deckPtr++;
        }
        return topCard;
    }

    /**getIndex
     * @param cards the cards to search
     * @param toFind the card to find
     * @return the index of the card if any
     * Searches through an array of cards for a specific card
     * and returns the index if found, otherwise returns -1
     */
    private int getIndex(String[] cards, String toFind) {
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) break;
            if (cards[i].equalsIgnoreCase(toFind)) return i;
        }
        return -1;
    }

    private int getSuit(String card) {
        String suit = card.substring(1, 2);
        if (isLeft(card)) {
            if (trump % 2 == 0) {
                return trump + 1;
            } else {
                return trump - 1;
            }
        }
        for (int i = 0; i < suits.length; i++) {
            if (suit.equals(suits[i])) return i;
        }
        return -1;
    }

    /**isLeft
     * @param card the card to check if it is the left
     * @return whether or not the card is the left
     * Checks to see if a given card is the jack of the
     * same color as trump
     */
    private boolean isLeft(String card) {
        String val = card.substring(0, 1);
        if (!val.equalsIgnoreCase("J")) return false;
        int suit = getSuit(card);
        if (trump % 2 == 0) {
            if (suit == trump + 1) return true;
        } else {
            if (suit == trump - 1) return true;
        }
        return false;
    }

    /**canPlay
     * @param card the card to be checked if the player can play
     * @return whether or not the card is a legal play
     * scans the current players hand to see if the passed card
     * is a legitimate play
     */
    private boolean canPlay(String card) {
        if (lead == -1) return true;
        int suit = getSuit(card);
        if (isLeft(card)) suit = trump;
        if (suit == lead) return true;
        String[] hand = (String[])super.players[playerTurn].get("hand");
        for (int i = 0; i < hand.length; i++) {
            int handSuit = getSuit(hand[i]);
            if (isLeft(hand[i])) handSuit = trump;
            if (handSuit == lead) return false;
        }
        return true;
    }

    /**findWinner
     * @return the team that won the trick
     * Scans the trick to see which card has the
     * highest value by the rules of euchre
     */
    private int findWinner() {
        int winner = -1;
        int winVal = -1;
        for (int i = 0; i < 4; i++) {
            if (trick[i] == null) continue;
            boolean isTrump = (getSuit(trick[i]) == trump);
            if (isLeft(trick[i])) isTrump = true;
            int newVal = getValue(trick[i], isTrump);
            if (newVal > winVal) {
                winVal = newVal;
                winner = i;
            }
        }
        return winner;
    }

    /**getValue
     * @param card the card to check to value of
     * @param isTrump whether or not the card was trump
     * @return an integer value of the card
     * determines a play value of a given card in a given
     * situation
     */
    private int getValue(String card, boolean isTrump) {
        if (isTrump) {
            switch(card.charAt(0)) {
                case '9':
                    return 10;
                case 'T':
                    return 11;
                case 'Q':
                    return 12;
                case 'K':
                    return 13;
                case 'A':
                    return 14;
                case 'J':
                    return 14;
            }
        } else {
            for (int i = 0; i < vals.length; i++) {
                if (card.charAt(0) == vals[i].charAt(0)) return i;
            }
        }
        return -1;
    }

    /**wasLoner
     * @param team the team to check for a loner
     * @return boolean whether the team went alone
     * checks to see if one of the players in the team
     * had a null hand which represents a loner
     */
    private boolean wasLoner(int team) {
        return (super.players[team].get("hand") == null ||
                super.players[team + 2].get("hand") == null);
    }

    @Override
    public void process(JSONObject info) {
    	System.out.println("processing state " + state);
        if (state % 2 == 1) {
            if (state < 10) requestSignal();
        }
        String key = null;
        try {
        	key = info.get("action").toString();
        } catch(JSONException e) {
        	e.printStackTrace();
        }
        try {
        switch (state) {
            case 0:
                startRound();
                break;
            case 2:
                processCall1(info.get(key).toString());
                break;
            case 4:
                processDropCard(info.get(key).toString());
                break;
            case 6:
                processLoner((Boolean)info.get(key));
                break;
            case 8:
                processCall2(info.get(key).toString());
                break;
            case 10:
                processPlay(info.get(key).toString());
                break;
            case 11:
                processTrick();
                break;
            case 12: 
                processDeal();
                break;
        }
        } catch (JSONException e) {
        	e.printStackTrace();
        }
    }
    
    public Hashtable<String, Object> getUIInfo(String displayName) {
    	Hashtable<String, Object> props = new Hashtable<String, Object>();
    	int playerIndex = -1;
    	for (int i = 0; i < players.length; i++) {
    		if (players[i].get("name").toString().equals(displayName)) playerIndex = i;
    	}
    	props.put("index", playerIndex);
    	props.put("scores", scores);
    	props.put("tricksTaken", tricksTaken);
    	props.put("trick", trick);
    	if (trump != -1) {
    		props.put("trump", suits[trump]);
    	} else {
    		props.put("trump", "none");
    	}
    	return props;
    }
    
    public String getPartner(int playerIndex) {
    	int partnerIndex = playerIndex + 2;
    	if (partnerIndex > 3) partnerIndex -= 4;
    	return players[partnerIndex].get("name").toString();
    }
}
