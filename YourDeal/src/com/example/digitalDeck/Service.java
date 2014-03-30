package com.example.digitalDeck;

import java.net.Inet4Address;

public class Service {
	
	private String title;
	private String type;
	private int players;
	private Inet4Address[] addresses;
	private int port;
	
	public Service(String aGameTitle, String aGameType, int aPlayerCount, Inet4Address[] someAddresses, int aPort) {
		title = aGameTitle;
		type = aGameType;
		players = aPlayerCount;
		addresses = someAddresses;
		port = aPort;
	}
	
	public String getTitle() {
		return title;
	}

	public String getType() {
		return type;
	}
	
	public int getNumPlayers() {
		return players;
	}
	
	public Inet4Address getFirstIP() {
	    return addresses[0];
	}
	
	public Inet4Address[] getIPs() {
		return addresses;
	}
	
	public int getPort() {
		return port;
	}
}
