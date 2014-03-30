package com.example.digitalDeck;

/**RemotePlayer
 * @author Bradley Johns
 * A player object for a remote client, works identically
 * to a normal player object, but forwards changes to its
 * delegate in order to maintain synchronization between
 * devices
 */

import java.net.Socket;
import org.json.JSONException;
import org.json.JSONObject;

public class RemotePlayer extends Player {
    private NetworkingDelegate delegate;

    public RemotePlayer(String aName, NetworkingDelegate aDelegate) {
        super(aName);
        delegate = aDelegate;
    }
    
    /**updateProperties
     * @param update
     * Performs player.updateProperties and then sends the update to
     * the server to inform the client on the foreign device
     */
    @Override
    public void updateProperties(JSONObject update) {
        super.updateProperties(update);
        delegate.updatedPlayer(this, update);
    }

    /**put
     * Performs player.put and then sends the update to the server
     * in order to inform the client on the foreign device
     */
    @Override
    public void put(String key, Object value) {
        super.put(key, value);
        JSONObject update = new JSONObject();
        try {
			update.put(key, value);
		} catch (JSONException e) {
			e.printStackTrace();
		}
        
        delegate.updatedPlayer(this, update);
    }

    /**setDelegate
     * sets the delegate that the remotePlayer object
     * must forward updates to
     */
    public void setDelegate(NetworkingDelegate aDelegate) {
        delegate = aDelegate;
    }
}
