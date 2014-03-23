package com.example.digitalDeck;

/**Player
 * @author Bradley Johns
 * The basic object to represent a player in the app.
 * A player is a dictionary of key value pairs of information
 * that may be required by any given game
 */

import java.util.*;

public class Player {
	
	protected Hashtable<String, Object> properties;

    /**Player constructor
     * @param name the name of the player
     * creates a player by initializing the dictionary and
     * putting a value of the players name into it
     */
    public Player(String name) {
        properties = new Hashtable<String, Object>();
        properties.put("name", name);
    }

    /**put
     * @param key the key to be added to the dictionary
     * @param value the value to be added to the dictionary
     * Puts the passed key, value pair into the dictionary
     */
    public void put(String key, Object value) {
        properties.put(key, value);
    }

    /**get
     * @param key the key to get from the dictionary
     * @return the value associated with the key
     * gets the specified key from the dictionary and returns its value
     */
    public Object get(String key) {
        return properties.get(key);
    }

    /**updateProperties
     * @param update the key/value pairs to be updated within the dictionary
     * The update portion of the program called by outside methods to correct
     * player information in the game. Uses a helper method in order to better
     * handle recursive cases
     */
    public void updateProperties(Dictionary<String, Object> update) {
        helperUpdate(update, properties);
    }
    
    /**helperUpdate
     * @param update the key/value pairs to be updated within the dictionary
     * @param old the dictionary to update the values in
     * Replaces all values with the given keys in the dictionary with new values,
     * if key is not in the dictionary the pair is added and if the key links
     * to another dictionary the subdictionary is updated recursively
     */
    private void helperUpdate(Dictionary<String, Object> update, Dictionary<String, Object> old) {
        Enumeration<String> keys = update.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (update.get(key) instanceof Dictionary) {
                Object oldVal = old.get(key);
                Dictionary<String, Object> subDict = null;
                if (oldVal instanceof Dictionary) subDict = (Dictionary<String, Object>)oldVal;
                if (subDict != null) {
                    helperUpdate((Dictionary<String, Object>)update.get(key),
                                 (Dictionary<String, Object>)old.get(key));
                }
            }
            old.put(key, update.get(key));
        }
    }
    
    /**getProperties
     * @return the properties dictionary
     * returns the properties of the player object
     */
    private Dictionary<String, Object> getProperties() {
        return properties;
    }
}
