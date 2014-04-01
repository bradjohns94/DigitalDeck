package com.example.digitalDeck;

import android.app.Application;

public class YourDealApplication extends Application {
	public static NetworkingDelegate networkingDelegate;
	public static Service selectedService; // TODO: Make this go away
	public static Game game;
	public static Player localPlayer;
	public static UIDelegate currentUI;
	
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