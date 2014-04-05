package com.example.digitalDeck;

/**Player
 * @author Bradley Johns
 * The basic object to represent a player in the app.
 * A player is a dictionary of key value pairs of information
 * that may be required by any given game
 */

import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;

public class Player {
	protected JSONObject properties;

    /**Player constructor
     * @param name the name of the player
     * creates a player by initializing the dictionary and
     * putting a value of the players name into it
     */
    public Player(String name) {
        properties = new JSONObject();
        try {
			properties.put("name", name);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    }

    /**put
     * @param key the key to be added to the dictionary
     * @param value the value to be added to the dictionary
     * Puts the passed key, value pair into the dictionary
     */
    public void put(String key, Object value) {
        try {
			properties.put(key, value);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    }

    /**get
     * @param key the key to get from the dictionary
     * @return the value associated with the key
     * gets the specified key from the dictionary and returns its value
     */
    public Object get(String key) {
        try {
			return properties.get(key);
		} catch (JSONException e) {
			e.printStackTrace();
		}
        
        return null;
    }

    /**updateProperties
     * @param update the key/value pairs to be updated within the dictionary
     * The update portion of the program called by outside methods to correct
     * player information in the game. Uses a helper method in order to better
     * handle recursive cases
     */
    public void updateProperties(JSONObject update) {
        helperUpdate(update, properties);
        if (YourDealApplication.currentUI != null) { // TODO: Decouple this
            YourDealApplication.currentUI.updateUI();
        }
    }
    
    /**helperUpdate
     * @param update the key/value pairs to be updated within the dictionary
     * @param old the dictionary to update the values in
     * Replaces all values with the given keys in the dictionary with new values,
     * if key is not in the dictionary the pair is added and if the key links
     * to another dictionary the subdictionary is updated recursively
     */
    private void helperUpdate(JSONObject update, JSONObject old) {
    	Iterator<String> iterator = update.keys();
    	try {
    		while (iterator.hasNext()) {
    			String key = iterator.next();
    			if (update.get(key) instanceof Dictionary) {
    				Object oldVal = old.get(key);
    				if (oldVal instanceof JSONObject) {
    					helperUpdate((JSONObject)update.get(key), (JSONObject)old.get(key));
    				}
    			}
    			old.put(key, update.get(key));
    		}
    	}
    	catch (JSONException e) {
    		e.printStackTrace();
    	}
    }
}
