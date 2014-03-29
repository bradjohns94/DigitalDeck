package com.example.digitalDeck;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

import org.json.JSONObject;

public class War extends Game{
	
	private static final String[] vals = {"9", "T", "J", "Q", "K", "A"}; 
    private static final String[] suits = {"S", "C", "H", "D"};
	
	ArrayList<String> p1Hand;
	ArrayList<String> p2Hand;
	String[] trick;
	int playerTurn;
	int state;
	
	public War(String hostName, String gameTitle) {
		super(2, hostName, gameTitle);
		state = 0;
		playerTurn = 0;
		trick = new String[2];
		p1Hand = new ArrayList<String>();
		p2Hand = new ArrayList<String>();
	}
	
	public void startRound() {
		String[] deck = makeDeck();
		for (int i = 0; i < 12; i++) {
			p1Hand.add(deck[i]);
		}
		for (int i = 12; i < 24; i++) {
			p2Hand.add(deck[i]);
		}
		state = 1;
		Hashtable<String, Object> empty = new Hashtable<String, Object>();
		//process(empty);
	}
	
	public void processPlay() {
		if (playerTurn == 0) {
			
		}
	}
	
	public String[] makeDeck() {
        Random rand = new Random();
        String [] deck= new String[24];
        int val;
        int suit;
        for (int i = 0; i < 24; i++) {
            val = rand.nextInt(6);
            suit = rand.nextInt(4);
            String card = vals[val] + suits[suit];
            if (getIndex(deck, card) != -1) {
                deck[i] = card;
            } else {
                i--;
                continue;
            }
        }
        return deck;
    }
	
	private int getIndex(String[] cards, String toFind) {
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) break;
            if (cards[i].equalsIgnoreCase(toFind)) return i;
        }
        return -1;
    }
	
	@Override
	public void process(JSONObject info) {
		switch (state) {
			case 0:
				startRound();
				break;
			case 1:
				Hashtable<String, Object> updates = new Hashtable<String, Object>();
				updates.put("action", "turn");
				Hashtable<Object, Hashtable<String, Object>> toSend = new Hashtable<Object, Hashtable<String, Object>>();
				toSend.put(super.players[playerTurn], updates);
				super.sender.updateProperties(toSend);
				state = 2;
				break;
			case 2:
				//String action = info.get("action").toString();
				//if (action != null) processPlay();
				break;
			case 3:
				
		}
	}
}
