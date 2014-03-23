/**CreateGameActivity
 * @author Bradley Johns
 * Creates a local settings page for the game the user
 * wishes to host followed by a create game button that
 * starts a lobby for other users to view/join
 */

package com.example.digitalDeck;

import java.io.IOException;
import java.util.HashMap;

import com.example.yourdeal.R;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.NavUtils;
import android.view.*;
import android.content.*;
import android.preference.PreferenceManager;
import android.widget.*;
import android.view.View.OnClickListener;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.RadioGroup.OnCheckedChangeListener; 
import android.graphics.Color;

import javax.jmdns.*;

public class CreateGameActivity extends Activity implements OnClickListener, OnCheckedChangeListener {

    private static final String[] gameModes = {"Euchre", "Hearts"};
    private AlertDialog gameDialog;
    private String gameMode;
    private String nextGameMode;
    private TextView text;
    android.net.wifi.WifiManager.MulticastLock lock;
    JmDNS jmdns;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        gameMode = gameModes[0];
        nextGameMode = gameModes[0];
        setContentView(R.layout.activity_create_game); 
        text = new TextView(this);

        drawGameOptions(gameMode);

        setTitle("Create game");

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
		getMenuInflater().inflate(R.menu.create_game, menu);
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

    public void startLobby(View view) {
        EditText titleText = (EditText)findViewById(R.id.gameTitleText);
        String title = titleText.getText().toString();
        //TODO customize game type by specifics
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String hostName = prefs.getString("display_name", "Unknown");
        Game newGame = new Game(4, hostName, title);
        final HashMap<String, String> props = new HashMap<String, String>();
        props.put("gameType", gameMode);
        props.put("hostUser", hostName);
        props.put("playerCount", "1");
        props.put("gameTitle", title);
        new Thread(){public void run() {try {
        	jmdns = JmDNS.create();
			createService(props);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}}}.start();

        Intent toLobby = new Intent(this, LobbyActivity.class);
        Bundle playerBundle = new Bundle();
        String[] playerList = new String[newGame.getPlayers().length];
        for (int i = 0; i < playerList.length; i++) {
        	playerList[i] = newGame.getPlayers()[i].get("name").toString();
        }
        playerBundle.putStringArray(null, playerList);
        toLobby.putExtras(playerBundle);
        toLobby.putExtra("caller", "CreateGameActivity");
        startActivity(toLobby);
    }

    public void drawGameOptions(String gameType) {
        TableLayout table = (TableLayout)findViewById(R.id.gameOptions); 
        table.setColumnStretchable(1, true);
        TableRow row = (TableRow)findViewById(R.id.gameTypeRow); 
        row.removeView(text);
        text.setText(gameType + " >"); 
        text.setPadding(0, 0, 0, 10); 
        text.setGravity(Gravity.RIGHT); 
        text.setTextAppearance(this, android.R.style.TextAppearance_Large); 
        row.setOnClickListener(this); 
        row.addView(text);
    }

    public void createService(HashMap<String, String> props) throws IOException{
    	android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager)getSystemService(android.content.Context.WIFI_SERVICE);
    	lock = wifi.createMulticastLock("DefinitelyCats");
    	lock.setReferenceCounted(true);
    	lock.acquire();
    	ServiceInfo serviceInfo = ServiceInfo.create("_DigitalDeck._tcp.local.", props.get("gameTitle"), 36241, 0, 0, props);
    	jmdns.registerService(serviceInfo);
    }
    
    @Override
	protected void onDestroy() {
    	super.onDestroy();
    	//if(lock != null) lock.release();
    }
    
    @Override
	protected void onStop() {
    	if (jmdns != null) {
    		/*jmdns.unregisterAllServices();
            try {
                jmdns.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }*/
    	}
    	lock.release();
    	super.onStop();
    }
    
    @Override
	public void onClick(View clicked) {
        final TableRow clickedRow = (TableRow)clicked;
        clickedRow.setBackgroundColor(Color.parseColor("#CCCCCC"));
        if (clickedRow != null && clickedRow.equals(findViewById(R.id.gameTypeRow))) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setMessage("Select Game Type");
            dialogBuilder.setTitle("Game Settings");

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

            RadioGroup group = new RadioGroup(this);
            for (int i = 0; i < gameModes.length; i++) {
                RadioButton button = new RadioButton(this);
                button.setText(gameModes[i].toString());
                button.setTextAppearance(this, android.R.style.TextAppearance_Large);
                group.addView(button);
            }
            group.setLayoutParams(params);
            group.setOnCheckedChangeListener(this);

            dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                @Override
				public void onClick(DialogInterface dialog, int item) {
                    if (!nextGameMode.equals(gameMode)) {
                        drawGameOptions(nextGameMode);
                        gameMode = nextGameMode;
                    }
                    clickedRow.setBackgroundColor(Color.parseColor("#E6E6E6"));
                    gameDialog.dismiss();
                }
            });

            layout.addView(group);
            dialogBuilder.setView(layout);
            gameDialog = dialogBuilder.create();
            gameDialog.show();
        }
    }

    @Override
	public void onCheckedChanged(RadioGroup group, int checkId) {
        RadioButton checked = (RadioButton)group.findViewById(checkId);
        nextGameMode = checked.getText().toString();
    }
}
