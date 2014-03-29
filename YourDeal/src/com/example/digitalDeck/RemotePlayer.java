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
    public void updateProperties(Hashtable<String, Object> update) {
        super.updateProperties(update);
        Hashtable<Object, Hashtable<String, Object>> udate = new Hashtable<Object, Hashtable<String, Object>>();
        udate.put(this, update);
        sender.updateProperties(udate);
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
        Hashtable<Object, Hashtable<String, Object>> update = new Hashtable<Object, Hashtable<String, Object>>();
        update.put(this, newDict);
        sender.updateProperties(update);
    }

    /**setDelegate
     * sets the delegate that the remotePlayer object
     * must forward updates to
     */
    public void setDelegate(Server server) {
        sender = server;
    }
}
