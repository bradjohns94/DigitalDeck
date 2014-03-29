package com.example.digitalDeck;

import java.util.Hashtable;

import org.json.JSONObject;

/**Delegate
 * @author Bradley Johns
 * The interface to be applied to the server and client objects to promote abstraction such
 * that the remote player and player files do not know whether or not they are directly
 * communicating with the server or through a client
 */
public interface Delegate {

    /**start
     * @param euchre the UI for the game to interact with
     * starts the service in the game activity and begins gameplay
     */
    public void start(EuchreUIActivity euchre);
    
    /**updateProperties
     * @param updates the updates to be forwarded to the given game or player accordingly
     * Take a dictionary of a flag as to what to forward and a JSON dictionary to forward
     * and be interprated by the given object
     */
    public void updateProperties(Hashtable<Object, Hashtable<String, Object>> updates);

    /**forwardInfo
     * @param info the information to be forwarded to the given object
     * Forward information to the next step in the chain that leads to the
     * server game object, be that the server or the game object itself
     */
    public void forwardInfo(JSONObject info);
}
