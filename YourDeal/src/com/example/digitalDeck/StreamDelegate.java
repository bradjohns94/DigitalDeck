package com.example.digitalDeck;

import org.json.JSONObject;

public interface StreamDelegate {
    public void streamReceivedData(Stream aStream, JSONObject data);
}
