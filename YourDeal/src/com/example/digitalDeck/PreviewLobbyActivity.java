package com.example.digitalDeck;

import org.json.JSONObject;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.NavUtils;
import android.view.*;
import android.widget.*;
import android.view.ViewGroup.LayoutParams;
import android.graphics.Color;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;

public class PreviewLobbyActivity extends Activity implements UIDelegate, OnClickListener {
    Client client;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preview_lobby);

		// Show the Up button in the action bar.
		setupActionBar();
		this.setTitle("Connecting to game...");
		YourDealApplication.currentUI = this; // Immediately take control of the app, in case anything changes
		
		Service service = YourDealApplication.selectedService;
		
		System.out.println("creating game");
		RemoteGame newGame = new RemoteGame(service.getGameSize(), service.getTitle(), service.getType());
		System.out.println("creating client");
		client = new Client(newGame, service);
		YourDealApplication.networkingDelegate = client;
		YourDealApplication.game = newGame;
		
		newGame.setNetworkingDelegate(client);
		System.out.println("connecting to server");
		client.connect();
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.preview_lobby, menu);
		return true;
	}

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
		    YourDealApplication.networkingDelegate.lobbyIsClosing();
			NavUtils.navigateUpFromSameTask(this);
			return true;
        case R.id.action_settings:
            Intent toSettings = new Intent(this, SettingsActivity.class);
            startActivity(toSettings);
            return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void drawPlayers() {
        TableLayout table = (TableLayout)findViewById(R.id.currentPlayers);
        table.removeAllViews(); //Clear all previous views upon refresh
        LayoutParams tableParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        //Create and add new rows with player info
        for (Player aPlayer : YourDealApplication.game.getPlayers()) {
            //Draw the specified player
            TableRow row = new TableRow(this);
            row.setLayoutParams(tableParams);
            TextView name = new TextView(this);
            name.setText((String)aPlayer.get("name"));
            name.setTextAppearance(this, android.R.style.TextAppearance_Large);
            name.setPadding(0,0,0,10);
            row.addView(name);
            row.setPadding(0,20,0,0);
            table.addView(row);

            //Draw a line beneath the text
            View line = new View(this);
            line.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, 1));
            line.setBackgroundColor(Color.parseColor("#808080"));
        }
        setTitle(YourDealApplication.game.getTitle());
    }

    public void openLobby(View clicked) {        
        Intent toLobby = new Intent(this, LobbyActivity.class);
        startActivity(toLobby);
    }

    @Override
    public void updateUI() {
        // Make sure that UI updates happen on the main Thread.
        this.runOnUiThread(new Runnable() {
            public void run() {
                drawPlayers();
            }
        });
    }

    @Override
    public void lobbyIsClosing() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PreviewLobbyActivity.this);
                dialogBuilder.setCancelable(false);
                dialogBuilder.setMessage("The host has cancelled this game.");
                dialogBuilder.setNeutralButton("Find another game", PreviewLobbyActivity.this);
                dialogBuilder.show();
            }
        });
    }

    @Override
    public void gameIsStarting() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PreviewLobbyActivity.this);
                dialogBuilder.setCancelable(false);
                dialogBuilder.setMessage("The host has cancelled this game.");
                dialogBuilder.setNeutralButton("Find another game", PreviewLobbyActivity.this);
                dialogBuilder.show();
            }
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Intent toFindGame = new Intent(this, FindGameActivity.class);
        
        YourDealApplication.game = null;
        
        startActivity(toFindGame);
    }
}
