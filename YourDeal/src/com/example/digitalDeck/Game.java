/**Game
 * @author Bradley Johns
 * An object class containing the basic information
 * required for a game in the app
 */

package com.example.digitalDeck;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

public abstract class Game {
    
    protected int gameSize;
    protected ArrayList<Player> players;
    protected HashMap<String, Player> playersByName;
    protected String title;
    protected NetworkingDelegate networkingDelegate;
    protected Object input;

    /**Game constructor
     * Construct a game given a number of players,
     * the creating host, and a title
     * @param gameSize: the number of players in the game
     * @param gameHost: the host of the game
     * @param gameTitle: the title of the game
     */
    public Game(int aSize, String aTitle) {
        gameSize = aSize;
        players = new ArrayList<Player>();
        title = aTitle;
    }

    public abstract String getType();
    
    public abstract void start();
    public abstract void process(JSONObject info);

    /**addPlayer
     * if the game is not full adds a player to the game and
     * increments the player counter
     * @param newPlayer: the player to be added to the game
     * @return: whether or not a player was added
     */
    public boolean addPlayer(Player aPlayer) {
        if (this.isFull()) return false;
        
        players.add(aPlayer);
        playersByName.put((String)aPlayer.get("name"), aPlayer);
        networkingDelegate.addedPlayer(aPlayer);
        return true;
    }

    /**removePlayer
     * Searches for the player in the game and removes
     * them from the game if found
     * @param player: the player to remove
     */
    public void removePlayer(Player aPlayer) {
    	playersByName.remove(aPlayer.get("name"));
    	players.remove(aPlayer);
    	networkingDelegate.removedPlayer(aPlayer);
    }

    public int getNumPlayers() {
        return players.size();
    }

    public boolean isFull() {
        return players.size() >= gameSize;
    }

    public int getGameSize() {
        return gameSize;
    }

    public String getHost() {
        return players.get(0).get("name").toString();
    }

    public String getTitle() {
        return title;
    }

    @SuppressWarnings("unchecked")
	public ArrayList<Player> getPlayers() {
        return (ArrayList<Player>)players.clone();
    }
    
    public Player getPlayerNamed(String aName) {
    	return playersByName.get(aName);
    }

    public void setNetworkingDelegate(NetworkingDelegate aDelegate) {
        networkingDelegate = aDelegate;
    }
}
