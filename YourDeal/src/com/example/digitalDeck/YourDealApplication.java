package com.example.digitalDeck;

import android.app.Application;

public class YourDealApplication extends Application {
	public static NetworkingDelegate networkingDelegate;
	public static Game game;
	public static Player localPlayer;
	
	private static YourDealApplication singleton;
    public static YourDealApplication getInstance() {
        return singleton;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
    }
}