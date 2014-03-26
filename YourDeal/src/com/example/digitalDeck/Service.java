package com.example.digitalDeck;

public class Service {
	
	private String title;
	private String type;
	private int players;
	private String[] ips;
	private String port;
	
	public Service(String gameTitle, String gameType, int playerCount, String[] addresses, String portNum) {
		title = gameTitle;
		type = gameType;
		players = playerCount;
		ips = addresses;
		port = portNum;
	}
	
	public String getTitle() {
		return title;
	}

	public String getType() {
		return type;
	}
	
	public int getPlayers() {
		return players;
	}
	
	public String[] getIPs() {
		return ips;
	}
	
	public String getPort() {
		return port;
	}
}
