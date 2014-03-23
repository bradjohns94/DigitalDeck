/**FindGameActivity
 * @author Bradley Johns
 * The activity that provides a screen for which the
 * user to find existing games on the network of the
 * type that was selected by the parent activity,
 * SelectGameActivity. It also provides the option for
 * the user to create their own game, which leads to the
 * CreateGameActivity
 */

package com.example.digitalDeck;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.NavUtils;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.content.Intent;
import android.widget.*;
import android.graphics.Color;

import java.util.ArrayList;

import com.example.yourdeal.R;

public class FindGameActivity extends Activity implements OnClickListener{

    private ArrayList<NsdServiceInfo> availableServices;
    private ArrayList<TableRow> gameRows;
    private ArrayList<Game> games;
    NsdManager man;



    /**onCreate
     * Set the title of the activity and display the activity
     */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_find_game);
        gameRows = new ArrayList<TableRow>();
        games = new ArrayList<Game>();
        createDiscoveryListener(games);
        games.add(new Game(4, "Bruce Wayne", "Batgame"));
        games.get(0).addPlayer("Hal Jordan");
        games.get(0).addPlayer("Barry Allan");
        games.add(new Game(4, "Arthur Curry", "Go Fish with Aquaman"));
        games.get(1).addPlayer("Mera");
        games.get(1).addPlayer("Flounder");
		// Show the Up button in the action bar.
		setupActionBar();

        //get the text passed in from select game
        String title = "Games Near You";

        setTitle(title);
        drawGames();
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

    /**onCreateOptionsMenu
     * Allow the users to see the actionbar
     * @return if displaying the action bar was a success
     */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.find_game, menu);
		return true;
	}

    /**onOptionsItemSelected
     * Allow the user to navigate home if the home button is selected,
     * also allow them to access the settings page to alter global
     * settings for the app
     * @param item the item that was selected
     * @return if the operation was a success
     */

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
        case R.id.action_settings:
            Intent toSettings = new Intent(this, SettingsActivity.class);
            startActivity(toSettings);
            return true;
		}
		return super.onOptionsItemSelected(item);
	}

    /**hostGame
     * Transition the activity to the CreateGame
     * activity if the create game option was selected
     * @param view the view to transition from
     */

    public void hostGame(View view) {
        Intent toCreate = new Intent(this, CreateGameActivity.class);
        startActivity(toCreate);
    }

    /**findGames
     * finds local games and converts them into a proper list
     * of games for the user to access
     * Note that this will use a loop and not be so repetative once
     * networking is functional
     */

    private void drawGames() {
        TableLayout table = (TableLayout)findViewById(R.id.currentPlayers);
        table.setColumnStretchable(1, true);
        LayoutParams tableParams = new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);

        for (int i = 0; i < games.size(); i++) {
            TableRow row = new TableRow(this);
            row.setLayoutParams(tableParams);
            //Text view for the game title
            TextView title = new TextView(this);
            title.setText(games.get(i).getTitle());
            title.setTextAppearance(this, android.R.style.TextAppearance_Large);
            title.setPadding(0,0,0,10);
            row.addView(title);

            //Text view for the current number of players
            String statusText = games.get(i).getNumPlayers() + "/" + games.get(i).getSize() + " Players";
            TextView status = new TextView(this);
            status.setText(statusText);
            status.setTextAppearance(this, android.R.style.TextAppearance_Large);
            status.setGravity(Gravity.RIGHT);
            status.setPadding(0,0,0,10);
            row.addView(status);

            row.setPadding(0,20,0,0);
            row.setOnClickListener(this);
            gameRows.add(row);
            table.addView(row);

            View line = new View(this);
            line.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, 1));
            line.setBackgroundColor(Color.parseColor("#808080"));
            table.addView(line);
        }
    }

    public void createDiscoveryListener(ArrayList<Game> games) {
        GameListener mDiscoveryListener = new GameListener(games);
        Context c = this;
        man = (NsdManager)c.getSystemService(Context.NSD_SERVICE);
        man.discoverServices(
                "_DigitalDeck._tcp.", NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    @Override
	public void onClick(View clicked) {
        TableRow clickedRow = (TableRow)clicked;
        clickedRow.setBackgroundColor(Color.parseColor("#CCCCCC"));

        //Determine which index in the arraylist the game is
        int index = 0;
        for (int i = 0; i < gameRows.size(); i++) {
            if (gameRows.get(i).equals(clickedRow)) {
                index = i;
            }
        }

        Intent toPreviewLobby = new Intent(this, PreviewLobbyActivity.class);
        Bundle players = new Bundle();
        String[] playerList = new String[games.get(index).getPlayers().length];
        for (int i = 0; i < playerList.length; i++) {
        	playerList[i] = games.get(index).getPlayers()[i].get("name").toString();
        }
        players.putStringArray(null, playerList); //TODO make a legitimate key
        toPreviewLobby.putExtras(players);
        startActivity(toPreviewLobby);
    }

}
