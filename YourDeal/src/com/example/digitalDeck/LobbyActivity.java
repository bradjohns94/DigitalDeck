package com.example.digitalDeck;

import com.example.yourdeal.R;

import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.NavUtils;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
import android.view.ViewGroup.LayoutParams;
import android.content.Intent;

public class LobbyActivity extends Activity {

    private String[] gamePlayers;
    private boolean isHost;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lobby);

        Bundle playerBundle = getIntent().getExtras();
        gamePlayers = playerBundle.getStringArray(null);
        drawPlayers(gamePlayers);

        String caller = getIntent().getStringExtra("caller");
        isHost = false;
        if (caller.equals("CreateGameActivity")) isHost = true;

        if (isHost) {
            Button start = (Button)findViewById(R.id.start);
            start.setVisibility(View.VISIBLE);
        }

		// Show the Up button in the action bar.
		setupActionBar();
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
		getMenuInflater().inflate(R.menu.lobby, menu);
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
			NavUtils.navigateUpFromSameTask(this);
			return true;
        case R.id.action_settings:
            Intent toSettings = new Intent(this, SettingsActivity.class);
            startActivity(toSettings);
            return true;
		}
		return super.onOptionsItemSelected(item);
	}

    public void drawPlayers(String[] players) {
        TableLayout table = (TableLayout)findViewById(R.id.currentPlayers);
        table.removeAllViews(); //Clear all previous views upon refresh
        LayoutParams tableParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

        //Create and add new rows with player info
        for (int i = 0; i < players.length; i++) {
            if (players[i] == null) break; //Break when lobby ends

            //Draw the specified player
            TableRow row = new TableRow(this);
            row.setLayoutParams(tableParams);
            TextView name = new TextView(this);
            name.setText(players[i]);
            name.setTextAppearance(this, android.R.style.TextAppearance_Large);
            name.setPadding(0,0,0,10);
            row.addView(name);
            row.setPadding(0,20,0,0);
            table.addView(row);

            //Draw a line beneath the text
            View line = new View(this);
            line.setLayoutParams(new TableRow.LayoutParams(LayoutParams.FILL_PARENT, 1));
            line.setBackgroundColor(Color.parseColor("#808080"));
        }
        setTitle(players[0] + "'s Game");
    }

    public void startGame(View view) {
        for (int i = 0; i < gamePlayers.length; i++) {
            if (gamePlayers[i] == null) return;
        }
        //TODO start the game
    }
}
