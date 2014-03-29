package com.example.digitalDeck;

import java.util.ArrayList;
import java.util.Hashtable;

import com.example.yourdeal.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.view.*;

public class EuchreUIActivity extends Activity {
	
	private Player localPlayer;
	private EuchreGame euchre;
	private Delegate delegate;
	private String partner;
	private ArrayList<ImageView> clickable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_euchre_ui);
		clickable = new ArrayList<ImageView>();
		delegate = YourDealApplication.delegate;
		euchre = YourDealApplication.game;
		localPlayer = YourDealApplication.local;
		localPlayer.setUI(this);
		delegate.start(this);
	}
	
	public void updateUI() {
		System.out.println("Updating UI");
		String[] hand = (String[])localPlayer.get("hand");
		Hashtable<String, Object> UIProps = euchre.getUIInfo(localPlayer.get("name").toString());
		int index = (Integer)UIProps.get("index");
		int teamIndex = index % 2;
		if (partner == null) {
			partner = euchre.getPartner(index);
			TextView partnerText = (TextView)findViewById(R.id.partner);
			partnerText.setText("Partner: " + partner);
		}
		
		for (int i = 0; i < hand.length; i++) {
			if (hand[i] != null) {
				ImageView toDraw = null;
				String fileName = hand[i];
				if (fileName.charAt(0) == '9') fileName.replace('9', 'n');
				fileName = fileName.toLowerCase(); //because android naming rules
				int resID = getResources().getIdentifier(fileName, "drawable", "com.example.digitalDeck");
				switch (i) {
					case 0:
						toDraw = (ImageView)findViewById(R.id.hand1);
						break;
					case 1:
						toDraw = (ImageView)findViewById(R.id.hand2);
						break;
					case 2:
						toDraw = (ImageView)findViewById(R.id.hand3);
						break;
					case 3:
						toDraw = (ImageView)findViewById(R.id.hand4);
						break;
					case 4:
						toDraw = (ImageView)findViewById(R.id.hand5);
						break;
				}
				toDraw.setImageResource(resID);
			}
		}
		
		//Draw the current trump
		String trumpString = UIProps.get("trump").toString();
		ImageView trumpImg = (ImageView)findViewById(R.id.trumpPic);
		if (!trumpString.equals("none")) {
			switch (trumpString.charAt(0)) {
				case 'C':
					trumpImg.setImageResource(R.drawable.clubs);
					break;
				case 'S':
					trumpImg.setImageResource(R.drawable.spades);
					break;
				case 'H':
					trumpImg.setImageResource(R.drawable.hearts);
					break;
				case 'D':
					trumpImg.setImageResource(R.drawable.diamonds);
					break;
			}
			trumpImg.setVisibility(View.VISIBLE);
		} else {
			trumpImg.setVisibility(View.INVISIBLE);
		}
		
		//Draw the current trick
		String[] trick = (String[])UIProps.get("trick");
		int trickPosition = 0;
		for (int i = 0; i < trick.length; i++) {
			if (trick[i] != null) trickPosition++;
		}
		int start = 4 - trickPosition;
		ImageView toChange = null;
		for (int i = 0; i < trickPosition; i++) {
			int toDraw = start + i;
			String fileName = trick[i];
			if (fileName.charAt(0) == '9') fileName.replace('9', 'n');
			fileName = fileName.toLowerCase(); //because android naming rules
			int resID = getResources().getIdentifier(fileName, "drawable", "com.example.digitalDeck");
			switch (toDraw) {
				case 1:
					toChange = (ImageView)findViewById(R.id.leftPlayerCard);
					break;
				case 2:
					toChange = (ImageView)findViewById(R.id.partnerCard);
					break;
				case 3:
					toChange = (ImageView)findViewById(R.id.rightPlayerCard);
					break;
			}
			toChange.setImageResource(resID);
			toChange.setVisibility(View.VISIBLE);
		}
		for (int i = start - 1; i > 0; i--) {
			ImageView toDisable = null;
			switch (i) {
				case 1:
					toDisable = (ImageView)findViewById(R.id.leftPlayerCard);
					break;
				case 2:
					toDisable = (ImageView)findViewById(R.id.partnerCard);
					break;
				case 3:
					toDisable = (ImageView)findViewById(R.id.rightPlayerCard);
					break;
			}
			if (toDisable != null) toDisable.setVisibility(View.INVISIBLE);
		}
		
		//Draw trickCount information
		int[] trickCount = (int[])UIProps.get("tricksTaken");
		TextView tricksWon = (TextView)findViewById(R.id.tricksWon);
		TextView tricksLost = (TextView)findViewById(R.id.tricksLost);
		tricksWon.setText("Tricks Won: " + Integer.toString(trickCount[teamIndex]));
		tricksLost.setText("Tricks Lost: " + Integer.toString(trickCount[(index + 1) % 2]));
		
		//Draw scoring information
		int[] scores = (int[])UIProps.get("scores");
		TextView scoreFor = (TextView)findViewById(R.id.yourScore);
		TextView scoreAgainst = (TextView)findViewById(R.id.opponentScore);
		scoreFor.setText("Your Score: " + Integer.toString(scores[teamIndex]));
		scoreAgainst.setText("Opponent Score: " + Integer.toString(scores[(index + 1) % 2]));		
	}
	
	public void queryUser(Hashtable<String, Object> info) {
		String key = info.get("action").toString();
		if (key == null) return;
		if (key.equals("turn")) {
			//TODO create alertDialog on whether or not to pass
		} else if (key.equals("drop")) {
			String[] plays = (String[])info.get(key);
			String[] hand = (String[])localPlayer.get("hand");
		} else if (key.equals("lone")) {
			
		} else if (key.equals("call")) {
			
		} else if (key.equals("play")) {
			
		}
	}
	
	public void processInput(View clicked) {
		if (!(clicked instanceof ImageView)) return;
		ImageView image = (ImageView)clicked;
		if (clickable.contains(image)) {
			//TODO write valid click code
			clickable = new ArrayList<ImageView>();
		} else {
			//TODO write invalid click code
		}
	}
}