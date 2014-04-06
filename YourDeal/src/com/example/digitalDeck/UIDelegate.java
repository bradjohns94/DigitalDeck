package com.example.digitalDeck;

import org.json.JSONObject;

public interface UIDelegate {    
    // Called to let the UI know that there's new information.
    public void updateUI();
    
    // These methods are called when things happen on the remote end:
    public void lobbyIsClosing();
    public void gameIsStarting();
}
