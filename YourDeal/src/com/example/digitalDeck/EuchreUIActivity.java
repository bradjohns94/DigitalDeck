package com.example.digitalDeck;

import java.util.ArrayList;
import java.util.Hashtable;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.yourdeal.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.*;
import android.view.*;

/**EuchreUIActivity
 * @author Bradley Johns
 * The UI for the euchre game type itself, displays the users
 * hand, the trick so far, the top card if applicable, the number
 * of tricks taken by each team, each team's score, the current trump,
 * and the player who is displaying the UI's partner.
 */

public class EuchreUIActivity extends Activity {
	
	private Player localPlayer;
	private EuchreGame euchre;
	private Delegate delegate;
	private String partner;
	private ArrayList<ImageView> clickable;
	private ImageView clicked;
	private Hashtable<String, ImageView> imageByName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_euchre_ui);
		clickable = new ArrayList<ImageView>();
		clicked = null;
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
		
		//Draw the players current hand
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
				imageByName.put(hand[i], toDraw);
			}
		}
		for (int i = hand.length; i < 5; i++) {
			ImageView toDisable = null;
			switch (i) {
				case 0:
					toDisable = (ImageView)findViewById(R.id.hand1);
					break;
				case 1:
					toDisable = (ImageView)findViewById(R.id.hand2);
					break;
				case 2:
					toDisable = (ImageView)findViewById(R.id.hand3);
					break;
				case 3:
					toDisable = (ImageView)findViewById(R.id.hand4);
					break;
				case 4:
					toDisable = (ImageView)findViewById(R.id.hand5);
					break;	
			}
			if (toDisable != null) toDisable.setVisibility(View.INVISIBLE);
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
		
		//Draw the topCard if applicable
		String topCard = UIProps.get("topCard").toString();
		ImageView img = (ImageView)findViewById(R.id.topCard);
		if (topCard != null) {
			if (topCard.charAt(0) == '9') topCard.replace('9', 'n');
			topCard = topCard.toLowerCase(); //because android naming rules
			int resID = getResources().getIdentifier(topCard, "drawable", "com.example.digitalDeck");
			img.setImageResource(resID);
			img.setVisibility(View.VISIBLE);
			imageByName.put(topCard, img);
		} else {
			img.setVisibility(View.INVISIBLE);
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
		clickable = new ArrayList<ImageView>();
		if (key.equals("turn")) {
			String card = euchre.getUIInfo(localPlayer.get("name").toString()).get("topCard").toString();
			String[] options = {"Pick it up", "Pass"};
			String message = getSuit(card) + "was turned up";
			drawBooleanDialog(options, message, "Your Turn");
		} else if (key.equals("drop") || key.equals("play")) {
			String[] plays = (String[])info.get(key);
			for (int i = 0; i < plays.length; i++) {
				ImageView canClick = imageByName.get(plays[i]);
				if (canClick != null) clickable.add(canClick);
			}
		} else if (key.equals("lone")) {
			String[] options = {"Yes", "No"};
			drawBooleanDialog(options, "Go alone?", "");
		} else if (key.equals("call")) {
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage("Select Game Type");
            dialogBuilder.setTitle("Game Settings");
			String[] options = (String[])info.get(key);
			RadioGroup group = new RadioGroup(this);
			for (int i = 0; i < options.length; i++) {
				RadioButton button = new RadioButton(this);
				button.setText(options[i]);
				group.addView(button);
			}
			LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            group.setLayoutParams(params);
            layout.addView(group);
            dialogBuilder.setView(layout);
            dialogBuilder.show();
            //TODO actually do something with the response
		}
	}
	
	public void processInput(View clicked) {
		if (!(clicked instanceof ImageView)) return;
		ImageView image = (ImageView)clicked;
		if (clickable.contains(image)) {
			//TODO write valid click code
			if (clicked == null) {
				
			} else if (image.equals(clicked)) {
				
			}
		} else {
			//TODO write invalid click code
		}
	}
	
	public void drawBooleanDialog(String[] options, String message, String title) {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setMessage(message);
        dialogBuilder.setTitle(title);
        dialogBuilder.setPositiveButton(options[0], new DialogInterface.OnClickListener() {
        	@Override
			public void onClick(DialogInterface dialog, int item) {
        		JSONObject response = new JSONObject();
        		try {
        			response.put("action", "response");
        			response.put("response", "call");
        			euchre.processInfo(response);
        		} catch (JSONException e) {
        			e.printStackTrace();
        		}
        	}
        });
        dialogBuilder.setNegativeButton(options[1], new DialogInterface.OnClickListener() {
        	@Override
			public void onClick(DialogInterface dialog, int item) {
        		JSONObject response = new JSONObject();
        		try {
        			response.put("action", "response");
        			response.put("response", "pass");
        			euchre.processInfo(response);
        		} catch (JSONException e) {
        			e.printStackTrace();
        		}
        	}
        });
        dialogBuilder.show();
	}
	
	public String getSuit(String card) {
		char suitChar = card.charAt(1);
		switch (suitChar) {
			case 'S':
				return "Spades";
			case 'C':
				return "Clubs";
			case 'H':
				return "Hearts";
			case 'D':
				return "Diamonds";
		}
		return "";
	}
}