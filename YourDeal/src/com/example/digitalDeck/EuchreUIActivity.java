package com.example.digitalDeck;

import java.util.ArrayList;
import java.util.Hashtable;

import org.json.*;

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
	private boolean showMessage = false;

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

		imageByName = new Hashtable<String, ImageView>();
		nameByImage = new Hashtable<ImageView, String>();
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
		JSONArray hand = (JSONArray)localPlayer.get("hand");
		Object partnerObj = localPlayer.get("partner");
		String partner = "";
		if (partnerObj != null) partner = partnerObj.toString();
		Hashtable<String, Object> UIProps = game.getUIInfo(localPlayer);
		int index = (Integer)localPlayer.get("index");
		int teamIndex = index % 2;
		if (partner != null) {
			TextView partnerText = (TextView)findViewById(R.id.partner);
			partnerText.setText("Partner: " + partner);
		}

		//Draw the players current hand
		for (int i = 0; i < hand.length(); i++) {
			String card = null;
			try {
				card = hand.getString(i);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			if (card != null) {
				ImageView toDraw = null;
				String fileName = card;
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
				toDraw.setVisibility(View.VISIBLE);
				try {
					imageByName.put(hand.getString(i), toDraw);
					nameByImage.put(toDraw, hand.getString(i));
				} catch(JSONException e) {
					e.printStackTrace();
				}
			}
		}
		for (int i = hand.length(); i < 5; i++) {
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
		String trumpString = "none";
		if (UIProps.get("trump") != null) trumpString = UIProps.get("trump").toString();
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
		JSONArray trick = (JSONArray)UIProps.get("trick");
		int trickPosition = 0;
		//This is hacky and terrible
		if (trick != null) {
			for (int i = 0; i < trick.length(); i++) {
				try {
					if (trick.getString(i) != null) trickPosition++;
				} catch(JSONException e) {
					e.printStackTrace();
				}
			}
		}
		int start = 4 - trickPosition;
		ImageView toChange = null;
		if (trick != null) System.out.println("trick is: " + trick);
		for (int i = 0; i < trickPosition; i++) {
			if (start == 0) break;
			int toDraw = start + i;
			String fileName = "";
			try {
				fileName = trick.getString(i);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			if (fileName.charAt(0) == '9') fileName.replace('9', 'n');
			fileName = fileName.toLowerCase(); //because android naming rules
			if (fileName == null) { //This shouldn't be possible but its happening...
				trickPosition--;
				continue;
			}
			System.out.println("filename = " + fileName);
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
			default:
				System.out.println("Your logic is bad and you should feel bad");
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
		String topCard = null;
		if (UIProps.get("topCard") != null) topCard = UIProps.get("topCard").toString();
		ImageView img = (ImageView)findViewById(R.id.topCard);
		if (topCard != null && !topCard.equals("none")) {
			if (topCard.charAt(0) == '9') topCard.replace('9', 'n');
			topCard = topCard.toLowerCase(); //because android naming rules
			int resID = getResources().getIdentifier(topCard, "drawable", "com.example.digitalDeck");
			img.setImageResource(resID);
			img.setVisibility(View.VISIBLE);
			imageByName.put(topCard, img);
			nameByImage.put(img, topCard);
		} else if (topCard != null && topCard.equals("none")){
			img.setVisibility(View.INVISIBLE);
		}

		//Draw trickCount information
		JSONArray trickCount = (JSONArray)UIProps.get("tricksTaken");
		try {
			if (trickCount != null) trickCount = (JSONArray)trickCount.get(0); //Super hacky, but if it works it works
		} catch (JSONException e) {
			e.printStackTrace();
		}
		TextView tricksWon = (TextView)findViewById(R.id.tricksWon);
		TextView tricksLost = (TextView)findViewById(R.id.tricksLost);
		try {
			if (trickCount != null) {
				tricksWon.setText("Tricks Won: " + Integer.toString(trickCount.getInt(teamIndex)));
				tricksLost.setText("Tricks Lost: " + Integer.toString(trickCount.getInt((index + 1) % 2)));
			}
		} catch(JSONException e) {
			e.printStackTrace();
		}

		//Draw scoring information
		JSONArray scores = (JSONArray)UIProps.get("scores");
		try {
			if (scores != null) scores = (JSONArray)scores.get(0); //Super hacky, but if it works it works
		} catch (JSONException e) {
			e.printStackTrace();
		}
		TextView scoreFor = (TextView)findViewById(R.id.yourScore);
		TextView scoreAgainst = (TextView)findViewById(R.id.opponentScore);
		try {
			if (scores != null) {
				scoreFor.setText("Your Score: " + Integer.toString(scores.getInt(teamIndex)));
				scoreAgainst.setText("Opponent Score: " + Integer.toString(scores.getInt((index + 1) % 2)));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		TextView message = (TextView)findViewById(R.id.message);
		if (!showMessage) message.setVisibility(View.INVISIBLE);

		try {
			JSONObject query = new JSONObject();
			Object action = localPlayer.get("action");
			query.put("action", action);
			queryUser(query);
		} catch (JSONException e) {
			e.printStackTrace();
		}
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
		String key = null;
		try {
			key = info.get("action").toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		//localPlayer.put("action", ""); //Try to stop multiple dialogs from forming
		System.out.println("Querying user");
		TextView displayMessage = (TextView)findViewById(R.id.message);
		if (key == null) return;
		//clickable = new ArrayList<ImageView>();
		if (key.equals("turn")) { //Prompt the user to pass or have the dealer pick it up
			String card = (String)game.getUIInfo(localPlayer).get("topCard");
			String[] options = {"Pick it up", "Pass"};
			String message = getSuit(card.charAt(1)) + " was turned up";
			drawBooleanDialog(options, message, "Your Turn");
		} else if (key.equals("drop") || key.equals("play")) { //Provide the user with a list of playable cards and wait on a response
			String message = "";
			if (key.equals("drop")) {
				System.out.println("Querying drop");
				message = "Select a Card to Put Down";
			} else {
				System.out.println("Querying play");
				message = "Select a Card to Play";
			}
			clickable = new ArrayList<ImageView>();
			showMessage = true;
			displayMessage.setText(message);
			displayMessage.setVisibility(View.VISIBLE);
			JSONArray plays = (JSONArray)localPlayer.get("validCards");
			try {
				plays = plays.getJSONArray(0);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			System.out.println("Entering for loop with JSONArray: " + plays);
			for (int i = 0; i < plays.length(); i++) {
				ImageView canClick = null;
				try {
					canClick = imageByName.get(plays.getString(i));
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if (canClick != null) clickable.add(canClick);
			}
			System.out.println("Clickable list is now: " + clickable);
		} else if (key.equals("lone")) { //Prompt the user on whether or not they wish to go alone
			String[] options = {"Yes", "No"};
			drawBooleanDialog(options, "Go alone?", "");
		} else if (key.equals("call")) { //Prompt the user to choose trump from the list provided or to pass
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
			dialogBuilder.setMessage("Select Game Type");
			dialogBuilder.setTitle("Game Settings");
			//String[] options = (String[])info.get(key);
			JSONArray JSONOptions = (JSONArray)localPlayer.get("validCalls");
			String[] options = new String[JSONOptions.length()];
			for (int i = 0; i < options.length; i++) {
				try {
					options[i] = getSuit(JSONOptions.getString(i).charAt(0));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
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
						String selected = handler.getSelected().substring(0,1);
						response.put("response", selected);
						game.process(response);
						dialog.cancel();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			});
			dialogBuilder.setNegativeButton("Pass", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					try {
						JSONObject response = new JSONObject();
						response.put("action", "response");
						response.put("response", "pass");
						game.process(response);
						dialog.cancel();
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
			dialogBuilder.setCancelable(false);
			localPlayer.put("action", "");
			dialogBuilder.show();
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
		System.out.println("Card was clicked!");
		if (!(pressed instanceof ImageView)) return;
		ImageView image = (ImageView)pressed;
		if (clickable.contains(image)) {
			System.out.println("Image was in clickable");
			if (clicked == null || !image.equals(clicked)) { //If the card has not received its first click
				System.out.println("Valid card was clicked for the first time!");
				clicked = image;
				image.setPadding(0, 0, 0, 5);
			} else { //Second click for confirmation
				System.out.println("Valid card was clicked for the second time!");
				clicked = null;
				clickable = new ArrayList<ImageView>();
				String play = nameByImage.get(image);
				localPlayer.put("action", "");
				try {
					JSONObject response = new JSONObject();
					response.put("action", "response");
					response.put("response", play);
					showMessage = false;
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
					System.out.println("Sending response to game");
					game.process(response);
					dialog.cancel();
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
					System.out.println("Sending response to game");
					game.process(response);
					dialog.cancel();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
		dialogBuilder.setCancelable(false);
		AlertDialog dialog = dialogBuilder.create();
		localPlayer.put("action", "");
		if (!dialog.isShowing()) dialog.show();
	}

	/**getSuit
	 * @param card the card to get the suit of
	 * @return the suit of the passed card
	 * get the suit of the card passed by the user
	 */
	public String getSuit(char suitChar) {
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
			RadioButton checked = (RadioButton)group.findViewById(id);
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