package com.example.digitalDeck;

import java.util.ArrayList;
import java.util.Hashtable;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.*;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.view.*;

/**EuchreUIActivity
 * @author Bradley Johns
 * The UI for the euchre game type itself, displays the users
 * hand, the trick so far, the top card if applicable, the number
 * of tricks taken by each team, each team's score, the current trump,
 * and the player who is displaying the UI's partner.
 */

public class EuchreUIActivity extends Activity implements UIDelegate {
	private Player localPlayer;
	private Game game;
	private String partner;
	private ArrayList<ImageView> clickable;
	private ImageView clicked;
	private Hashtable<String, ImageView> imageByName;
	private Hashtable<ImageView, String> nameByImage;

	/**onCreate
	 * @param savedInstance used by eclipse when creating the activity
	 * initializes the memeber variables of the class and starts the game
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_euchre_ui);
		
		YourDealApplication.currentUI = this;
		
		clickable = new ArrayList<ImageView>();
		clicked = null;
		game = YourDealApplication.game;
		localPlayer = YourDealApplication.localPlayer;
		game.start();
	}
	
	/**updateUI
	 * Analyzes all components for the UI to display from the getUIInfo class of the game object
	 * then draws them to the screen. Displays include the following:
	 * 1. The players current hand, setting cards that are currently there visible while turning all other views for the hand off
	 * 2. The current trump if applicable, if no trump has been chosen for the round yet the trump image is set as invisible
	 * 3. The current trick from the perspective of the local player showing the next player in the array as to their left
	 * 4. The top card of the deal if applicable, if not the card is set as invisible
	 * 5. TrickCount information, show how many tricks the local players team has won and lost
	 * 6. Score show the current score for both teams in the game
	 */
	public void drawGame() {
		System.out.println("Updating UI");
		String[] hand = (String[])localPlayer.get("hand");
		Hashtable<String, Object> UIProps = game.getUIInfo(localPlayer);
		int index = (Integer)UIProps.get("index");
		int teamIndex = index % 2;
		if (partner == null) {
			partner = (String)UIProps.get("partner");
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
				
				// TODO: plz no
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
				if (toDraw.equals(clicked)) {
					toDraw.setPadding(0, 0, 0, 5);
				} else {
					toDraw.setPadding(0, 0, 0, 0);
				}
				toDraw.setImageResource(resID);
				imageByName.put(hand[i], toDraw);
				nameByImage.put(toDraw, hand[i]);
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
			nameByImage.put(img, topCard);
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
		
		TextView message = (TextView)findViewById(R.id.message);
		message.setVisibility(View.INVISIBLE);
	}
	
	/**queryUser
	 * @param info a hashtable of information used to query the user
	 * Decides based on the information type received from the "action" key of the passed
	 * hash table what type of message to display of 4 different types
	 * 1. Ask the player whether they want to set the suit of the top card as trump
	 * 2. Ask the player to play or put down a card from their hand
	 * 3. Prompt the player to whether or not they would like to go alone
	 * 4. Prompt the user on to whether or not they would like to decide trump
	 */
	public void queryUser(JSONObject info) {
		try {
            String key = info.get("action").toString();
            TextView displayMessage = (TextView)findViewById(R.id.message);
            if (key == null) return;
            clickable = new ArrayList<ImageView>();
            if (key.equals("turn")) { //Prompt the user to pass or have the dealer pick it up
            	String card = (String)game.getUIInfo(localPlayer).get("topCard");
            	String[] options = {"Pick it up", "Pass"};
            	String message = getSuit(card) + "was turned up";
            	drawBooleanDialog(options, message, "Your Turn");
            } else if (key.equals("drop") || key.equals("play")) { //Provide the user with a list of playable cards and wait on a response
            	String message = "";
            	if (key.equals("drop")) {
            		message = "Select a Card to Put Down";
            	} else {
            		message = "Select a Card to Play";
            	}
            	displayMessage.setText(message);
            	displayMessage.setVisibility(View.VISIBLE);
            	String[] plays = (String[])info.get(key);
            	for (int i = 0; i < plays.length; i++) {
            		ImageView canClick = imageByName.get(plays[i]);
            		if (canClick != null) clickable.add(canClick);
            	}
            } else if (key.equals("lone")) { //Prompt the user on whether or not they wish to go alone
            	String[] options = {"Yes", "No"};
            	drawBooleanDialog(options, "Go alone?", "");
            } else if (key.equals("call")) { //Prompt the user to choose trump from the list provided or to pass
            	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setMessage("Select Game Type");
                dialogBuilder.setTitle("Game Settings");
            	String[] options = (String[])info.get(key);
            	RadioGroup group = new RadioGroup(this);
            	for (int i = 0; i < options.length; i++) {
            		if (options[i].equals("pass")) continue;
            		RadioButton button = new RadioButton(this);
            		button.setText(options[i]);
            		group.addView(button);
            	}
            	final RadioHandler handler = new RadioHandler();
            	group.setOnCheckedChangeListener(handler);
            	dialogBuilder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
            		@Override
            		public void onClick(DialogInterface dialog, int which) {
            			try {
            				JSONObject response = new JSONObject();
            				response.put("action", "response");
            				response.put("response", handler.getSelected());
            				game.process(response);
            			} catch (JSONException e) {
            				e.printStackTrace();
            			}
            		}
            	}); //TODO make this forward the selected
            	dialogBuilder.setNegativeButton("Pass", new DialogInterface.OnClickListener() {
            		@Override
            		public void onClick(DialogInterface dialog, int which) {
            			try {
            				JSONObject response = new JSONObject();
            				response.put("action", "response");
            				response.put("response", "pass");
            				game.process(response);
            			} catch (JSONException e) {
            				e.printStackTrace();
            			}
            		}
            	});
            	LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                group.setLayoutParams(params);
                layout.addView(group);
                dialogBuilder.setView(layout);
                dialogBuilder.show();
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
	}
	
	/**processInput
	 * @param pressed the view of the clicked object
	 * checks whether the pressed card is of valid use to the game object at the current moment,
	 * if it is the method checks the card with the last clicked object, if they match it plays the
	 * card, otherwise it marks it as the last clicked object for the gameUI to display. If the play
	 * is invalid this method should inform the user of such.
	 */
	public void processInput(View pressed) {
		if (!(pressed instanceof ImageView)) return;
		ImageView image = (ImageView)pressed;
		if (clickable.contains(image)) {
			if (clicked == null || !image.equals(clicked)) { //If the card has not received its first click
				clicked = image;
				image.setPadding(0, 0, 0, 5);
			} else { //Second click for confirmation
				clicked = null;
				clickable = new ArrayList<ImageView>();
				String play = nameByImage.get(image);
				try {
					JSONObject response = new JSONObject();
					response.put("action", "response");
					response.put("response", play);
					game.process(response);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} else {
			//TODO write invalid click code
		}
	}
	
	/**drawBooleanDialog
	 * @param options the options to give the dialog
	 * @param message the message for the dialog to display
	 * @param title the title of the dialog
	 * Prompts the user with a yes/no dialog based on the information
	 * provided
	 */
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
        			game.process(response);
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
        			game.process(response);
        		} catch (JSONException e) {
        			e.printStackTrace();
        		}
        	}
        });
        dialogBuilder.show();
	}
	
	/**getSuit
	 * @param card the card to get the suit of
	 * @return the suit of the passed card
	 * get the suit of the card passed by the user
	 */
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
	
	/**RadioHandler
	 * @author Bradley Johns
	 * handles the onCheckedChanged of an alertDialog and updates
	 * a String representing the currently chosen option, this
	 * option can then be accessed by the rest of the program by
	 * the getSelected() method
	 */
	private class RadioHandler implements OnCheckedChangeListener {
		
		private String selected;
		
		/**RadioHandler constructor
		 * initializes the selected variable to pass if the user never
		 * presses a button
		 */
		public RadioHandler() {
			selected = "pass";
		}
		
		/**onCheckedChanged
		 * @param group the group that had its checked option changed
		 * @param id the id of the changed object
		 * changes the selected string to that of the selected radioButton
		 */
		public void onCheckedChanged(RadioGroup group, int id) {
			RadioButton checked = (RadioButton)findViewById(id);
			selected = checked.getText().toString();
		}
		
		/**getSelected
		 * @return the string of the selected radioButton
		 * gives the rest of the program access to the selected radioButton
		 */
		public String getSelected() {
			return selected;
		}
	}

    @Override
    public void lobbyIsClosing() {
        // TODO: Use this for quitting the game
    }

    @Override
    public void gameIsStarting() {
        // Probably not useful
    }

    @Override
    public void updateUI() {
        runOnUiThread(new Runnable() {
            public void run() {
                drawGame();
            }
        });
    }
}