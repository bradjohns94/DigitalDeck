package com.example.digitalDeck;

import org.json.JSONObject;

public class RemoteGame extends Game {
	protected String type;

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
}
