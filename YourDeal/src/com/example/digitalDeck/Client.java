package com.example.digitalDeck;

import java.io.IOException;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Int2;

public class Client implements NetworkingDelegate, StreamDelegate {
	private RemoteGame game;
	private Service service;
	private Stream stream;
	
	public Client(RemoteGame aRemoteGame, Service aService) {
		game = aRemoteGame;
		service = aService;
	}
	
	public void connect() {
	    new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                
                try {
                    Socket socket = new Socket(service.getFirstIP(), service.getPort());
                    stream = new Stream(socket, Client.this, Looper.myLooper());
                    
                    JSONObject requestPlayers = new JSONObject();
                    requestPlayers.put("request", "playerList");
                    stream.queueWrite(requestPlayers);
                    System.out.println("wrote player request");
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
                
                Looper.loop();
            }
	    }).start();
	}
	
	public void disconnect() {
	    try {
            JSONObject data = new JSONObject();
            data.put("event", "leaving");
            stream.queueWrite(data);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
	    // TODO: Actually close this stream
	    //stream.stop();
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
        
        System.out.println("received: " + data);
        
        try {
            if (data.get("target").equals("game")) {
                if (data.has("addPlayer")) {
                    if (YourDealApplication.localPlayer == null || !YourDealApplication.localPlayer.get("name").equals(data.get("addPlayer"))) {
                        // Only add the player if it isn't us.
                        // TODO: This is probably a hack
                        game.addPlayer(new RemotePlayer((String)data.get("addPlayer"), this));
                    }
                }
                
                if (data.has("removePlayer")) {
                    game.removePlayer(game.getPlayerNamed((String)data.get("removePlayer")));
                }
                //This part commented out by brad
                //if (data.has("action")) {
                    game.process(data);
                //}
            }
            else if (data.get("target").equals("player")) {
                YourDealApplication.localPlayer.updateProperties(data);
            }
            else if (data.get("target").equals("client")) {
                // We've received a meta message.
                if (data.get("event").equals("lobbyIsClosing")) {
                    lobbyIsClosing();
                    YourDealApplication.currentUI.lobbyIsClosing();
                }
                else if (data.get("event").equals("gameIsStarting")) {
                    YourDealApplication.currentUI.gameIsStarting();
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        
        if (YourDealApplication.currentUI != null) {
            System.out.println("updating ui from client.streamReceivedData");
            System.out.println("currentUI = " + YourDealApplication.currentUI);
            YourDealApplication.currentUI.updateUI(); // This will run on the correct Thread.
        }
        else {
            System.out.println("WHAT THE HECK");
        }
    }

    @Override
    public void lobbyIsClosing() {
        disconnect();
    }

    @Override
    public void gameIsStarting() {
        JSONObject signal = new JSONObject();
        try {
        	signal.put("event", "readyToPlay");
        } catch (JSONException e) {
        	e.printStackTrace();
        }
        stream.queueWrite(signal);
    }
}
