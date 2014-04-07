package com.example.digitalDeck;

import java.util.Hashtable;

import org.json.JSONObject;

public class RemoteGame extends Game {
	private String type;
	private Hashtable<String, Object> uiInfo;

	/**RemoteGame
	 * @param aSize: The size of the game
	 * @param aTitle: The title of the game
	 * @param aType: The type of game to impersonate
	 */
	public RemoteGame(int aSize, String aTitle, String aType) {
		super(aSize, aTitle);
		type = aType;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public void process(JSONObject info) {
		networkingDelegate.updatedGame(info);
	}

    @Override
    public void start() {
        
    }

    @Override
    public Hashtable<String, Object> getUIInfo(Player aPlayer) {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.putAll(uiInfo);
        
        // TODO: This should just be data stored on the Player
        int playerIndex = players.indexOf(aPlayer);
        int partnerIndex = (playerIndex + 2) % 4;
        String partnerName = players.get(partnerIndex).get("name").toString();
        
        props.put("index", playerIndex);
        props.put("partner", partnerName);
        
        return props;
    }
}
