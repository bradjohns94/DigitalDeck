package com.example.digitalDeck;

import org.json.JSONException;
import org.json.JSONObject;

public class Client implements NetworkingDelegate, StreamDelegate {
	private RemoteGame game;
	private Service service;
	private Stream stream;
	
	public Client(RemoteGame aRemoteGame, Service aService) {
		game = aRemoteGame;
		service = aService;
	}
	
	public void connect() {
	    //Start a connection to the server
        stream = new Stream(service, this);
        
        JSONObject requestPlayers = new JSONObject();
        try {
            requestPlayers.put("request", "playerList");
            stream.queueWrite(requestPlayers);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
	}
	
	public void disconnect() {
	    stream.stop();
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
        if (aPlayer != YourDealApplication.localPlayer) return;
        
        try {
            System.out.println("forwarding player info to server: " + aPlayer.get("name"));
            JSONObject data = new JSONObject();
            data.put("target", "game");
            data.put("addPlayer", aPlayer.get("name"));
            stream.queueWrite(data);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removedPlayer(Player aPlayer) {
        if (aPlayer != YourDealApplication.localPlayer) return;
        
        try {
            JSONObject data = new JSONObject();
            data.put("target", "game");
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
        
        System.out.println("received:");
        System.out.println(data);
        
        try {
            if (data.get("target").equals("game")) {
                if (data.has("addPlayer")) {
                    game.addPlayer(new RemotePlayer((String)data.get("addPlayer"), this));
                }
                
                if (data.has("removePlayer")) {
                    game.removePlayer(game.getPlayerNamed((String)data.get("removePlayer")));
                }
                
                if (data.has("action")) {
                    game.process(data);
                }
            }
            else if (data.get("target").equals("player")) {
                YourDealApplication.localPlayer.updateProperties(data);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        
        if (YourDealApplication.currentUI != null) {
            YourDealApplication.currentUI.updateUI();
        }
        else {
            System.out.println("WHAT THE HECK");
        }
    }
    
    public boolean isUserGiantPurpleMonsterThingyMagigitWhatAmIWritingRightNow() {
        return false;
    }
}
