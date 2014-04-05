package com.example.digitalDeck;

import org.json.JSONObject;

/**Delegate
 * @author Bradley Johns
 * The interface to be applied to the server and client objects to promote abstraction such
 * that the remote player and player files do not know whether or not they are directly
 * communicating with the server or through a client
 */
public interface NetworkingDelegate {
	public Game getGame();
	
    /**isHostingGame
     * @return true if this is a Server, false if this is a Client.
     */
    public boolean isHostingGame();
    
    /**lobbyIsClosing
     * Called by the lobby when the user has decided to leave.
     */
    public void lobbyIsClosing();
    
    /* Both the Server and Client implement all of these methods.
     * On the Server, they are called by the Game and by RemotePlayers to forward info to Clients.
     * On the Client, they are called by the RemoteGame to send information back to the Server, and updatedPlayer() is never actually used. A casualty of convenience.
     */
    public void addedPlayer(Player aPlayer);
    public void removedPlayer(Player aPlayer);
    public void updatedPlayer(RemotePlayer aPlayer, JSONObject updates);
    public void updatedGame(JSONObject updates);
}
