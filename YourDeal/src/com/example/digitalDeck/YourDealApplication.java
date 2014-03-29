package com.example.digitalDeck;

import android.app.Application;

public class YourDealApplication extends Application {
	
	public static Delegate delegate;
	public static EuchreGame game; //We'll adjust this later
	public static Player local;
}