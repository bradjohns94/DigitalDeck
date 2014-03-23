package com.example.digitalDeck;

/**RemotePlayer
 * @author Bradley Johns
 * A player object for a remote client, works identically
 * to a normal player object, but forwards changes to its
 * delegate in order to maintain synchronization between
 * devices
 */

import java.util.*;

public class RemotePlayer extends Player{

    private Server sender;

    public RemotePlayer(String name) {
        super(name);
    }
    
    /**updateProperties
     * @param update
     * Performs player.updateProperties and then sends the update to
     * the server to inform the client on the foreign device
     */
    @Override
    public void updateProperties(Dictionary<String, Object> update) {
        super.updateProperties(update);
        sender.updateProperties(this, update);
    }

    /**put
     * Performs player.put and then sends the update to the server
     * in order to inform the client on the foreign device
     */
    @Override
    public void put(String key, Object value) {
        super.put(key, value);
        Hashtable<String, Object> newDict = new Hashtable<String, Object>();
        newDict.put(key, value);
        sender.updateProperties(this, newDict);
    }

    /**setDelegate
     * sets the delegate that the remotePlayer object
     * must forward updates to
     */
    public void setDelegate(Server server) {
        sender = server;
    }
}
