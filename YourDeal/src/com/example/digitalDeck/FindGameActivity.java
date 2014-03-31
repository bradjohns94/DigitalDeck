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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.content.Intent;
import android.widget.*;
import android.graphics.Color;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;

import javax.jmdns.*;

public class FindGameActivity extends Activity implements OnClickListener {

    private ArrayList<TableRow> gameRows;
    private ArrayList<Service> services;
    android.net.wifi.WifiManager.MulticastLock lock;
    JmDNS jmdns;
    
    /**onCreate
     * Set the title of the activity and display the activity
     */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_find_game);
        gameRows = new ArrayList<TableRow>();
        services = new ArrayList<Service>();
        
		// Show the Up button in the action bar.
		setupActionBar();
		setTitle("Games Near You");
        
        new Thread() {
            public void run() {
            	try {
                	jmdns = JmDNS.create();
                    addServices(jmdns.list("_DigitalDeck._tcp.local."));
                    jmdns.addServiceListener("_DigitalDeck._tcp.local.", new FindGameListener());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        drawGames();
	}
	
	/** * Set up the {@link android.app.ActionBar}.  */
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
    	//lock.release();
    	super.onStop();
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

        for (Service service : services) {
            System.out.println("drawing service: " + service);
            TableRow row = new TableRow(this);
            row.setLayoutParams(tableParams);
            //Text view for the game title
            TextView title = new TextView(this);
            title.setText(service.getTitle());
            title.setTextAppearance(this, android.R.style.TextAppearance_Large);
            title.setPadding(0,0,0,10);
            row.addView(title);

            //Text view for the current number of players
            String statusText = service.getNumPlayers() + "/" + /*games.get(i).getSize()*/4 + " Players";
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

        YourDealApplication.selectedService = services.get(index);
        
        Intent toPreviewLobby = new Intent(this, PreviewLobbyActivity.class);
        startActivity(toPreviewLobby);
    }
    
    private class FindGameListener implements ServiceListener {
        /*Delegation methods
         * They do magic JmDNS stuff
         * I'll talk more about these later
         */
        
        //TODO literally everything
        public void serviceAdded(ServiceEvent event) {
            //This does nothing
            System.out.println("Oh hai, I added a service");
        }
        
        public void serviceRemoved(ServiceEvent event) {
            ServiceInfo info = event.getInfo();
            String title = info.getName();
            for (Service g : services) {
                if (g.getTitle().equals(title)) {
                    services.remove(g);
                    break;
                }
            }
            runOnUiThread(new Runnable() {
                public void run() {
                    drawGames();
                }
            });
        }
        
        public void serviceResolved(ServiceEvent event) {
            //TODO create list item
            ServiceInfo info = event.getInfo();
            System.out.println("resolved");
            addService(info);
        }
    }
    
    private void addService(ServiceInfo info) {
    	ServiceInfo[] infos = { info };
    	System.out.println("adding service");
    	this.addServices(infos);
    }
    
    private void addServices(ServiceInfo[] someInfos) {
    	for (ServiceInfo info : someInfos) {
    	    System.out.println(info);
    		String title = info.getName();
            String gameType = info.getPropertyString("gameType");
            // TODO
            int gameSize = 4;//Integer.parseInt(info.getPropertyString("gameSize"));
            int playerCount = Integer.parseInt(info.getPropertyString("playerCount"));
            Inet4Address[] addresses = info.getInet4Addresses();
            int port = info.getPort();
            
            Service game = new Service(title, gameType, gameSize, playerCount, addresses, port);
            System.out.println(game);
            services.add(game);
    	}
    	
        runOnUiThread(new Runnable() {
            public void run() {
                drawGames();
            }
        });
    }
}
