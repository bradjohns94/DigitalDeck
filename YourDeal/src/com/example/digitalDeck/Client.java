package com.example.digitalDeck;

import org.json.JSONException;
import org.json.JSONObject;

public class Client implements NetworkingDelegate, StreamDelegate {
	private RemoteGame game;
	private Stream stream;
	
	public Client(RemoteGame aRemoteGame, Service aService) {
		game = aRemoteGame;
		
		//Start a connection to the server
        stream = new Stream(aService);
        stream.setDelegate(this);
	}
	
	public RemoteGame getGame() {
		return game;
	}

	@Override
	public boolean isHostingGame() {
		return false;
	}

    @Override
    public void addedPlayer(Player aPlayer) {
        try {
            JSONObject data = new JSONObject();
            data.put("addPlayer", aPlayer.get("name"));
            stream.queueWrite(data);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removedPlayer(Player aPlayer) {
        try {
            JSONObject data = new JSONObject();
            data.put("removePlayer", aPlayer.get("name"));
            stream.queueWrite(data);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updatedPlayer(RemotePlayer aPlayer, JSONObject updates) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updatedGame(JSONObject updates) { // This is sort of a lie on the Client end. Oh well.
        stream.queueWrite(updates);
    }

    @Override
    public void streamReceivedData(Stream aStream, JSONObject data) {
        if (!aStream.equals(stream)) {
            System.out.println("what the heck");
        }
    }
}
