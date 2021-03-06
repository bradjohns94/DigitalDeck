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
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.format.Formatter;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.content.Intent;
import android.database.DataSetObserver;
import android.widget.*;
import android.graphics.Color;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import javax.jmdns.*;

public class FindGameActivity extends Activity implements OnClickListener {
    // TODO: Rebuild this to use a ListView and ListAdapter instead of building the table manually.

    private ArrayList<TableRow> gameRows;
    private ArrayList<Service> services;
    private android.net.wifi.WifiManager.MulticastLock lock;
    private JmDNS jmdns;
    private ServiceListener listener;
    
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
        
		startListening();
        drawGames();
	}
	
	private void startListening() {
	    if (lock != null && lock.isHeld()) return;
	    
	    new Thread(new Runnable() {
            public void run() {
                WifiManager wifi = (WifiManager)getSystemService(WIFI_SERVICE);
                
                try {
                    if (lock == null) {
                        lock = wifi.createMulticastLock("DefinitelyCats");
                        lock.setReferenceCounted(true);
                    }
                    lock.acquire(); // Lock multicast open
                    
                    System.out.println("preparing to listen");
                    // Plain JmDNS.create() apparently does not work properly on all newer devices. Providing an address:
                    String ip = Formatter.formatIpAddress(wifi.getConnectionInfo().getIpAddress());
                    jmdns = JmDNS.create(InetAddress.getByName(ip));
                    System.out.println("created jmdns");
                    addServices(jmdns.list("_DigitalDeck._tcp.local."));
                    System.out.println("listed existing services");
                    
                    if (listener == null) {
                        listener = new FindGameListener();
                    }
                    jmdns.addServiceListener("_DigitalDeck._tcp.local.", listener);
                    System.out.println("listener registered");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
	}
	
	private void stopListening() {
	    if (lock == null || !lock.isHeld()) return;
	    System.out.println("stop of listen");
	    
        new Thread(new Runnable() {
            public void run() {
                if (jmdns != null && listener != null) {
                    // TODO: If we make it here before JmDNS has been registered, it will get registered
                    System.out.println("unregistering");
                    jmdns.removeServiceListener("_DigitalDeck._tcp.local.", listener);
                }
                
                if (lock.isHeld()) {
                    lock.release();
                }
            }
        }).start();
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
    protected void onRestart() {
        startListening();
        super.onResume();
    }
    
    @Override
	protected void onStop() {
    	stopListening();
    	super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        new Thread(new Runnable() {
            public void run() {
                if (jmdns != null) {
                    try {
                        listener = null;
                        jmdns.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("jmdns closed");
                }
            }
        });
        super.onDestroy();
    }

    /**findGames
     * finds local games and converts them into a proper list
     * of games for the user to access
     * Note that this will use a loop and not be so repetitive once
     * networking is functional
     */

    private void drawGames() {
        TableLayout table = (TableLayout)findViewById(R.id.currentPlayers);
        table.removeAllViews(); // Clean out existing services
        gameRows.clear(); // TODO: This needs to not exist
        table.setColumnStretchable(1, true);
        LayoutParams tableParams = new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1);

        for (Service service : services) {
            TableRow row = new TableRow(this);
            row.setLayoutParams(tableParams);
            // Text view for the game title
            TextView title = new TextView(this);
            title.setText(service.getTitle());
            title.setTextAppearance(this, android.R.style.TextAppearance_Large);
            title.setPadding(0,0,0,10);
            row.addView(title);

            // Text view for the current number of players
            String statusText = null;
            if (service.getGameSize() > 0) {
                statusText = service.getNumPlayers() + "/" + service.getGameSize() + " Players";
            }
            else {
                statusText = service.getNumPlayers() + " Players";
            }
            
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
            line.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, 1));
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
    
    private void addService(ServiceInfo info) {
    	ServiceInfo[] infos = { info };
    	this.addServices(infos);
    }
    
    private void addServices(ServiceInfo[] someInfos) {
        WifiManager wifi = (WifiManager)getSystemService(WIFI_SERVICE);
        String localIP = Formatter.formatIpAddress(wifi.getConnectionInfo().getIpAddress());
        
    	for (ServiceInfo info : someInfos) {
    	    // Don't list our own services, and don't list dead services (usually also our own).
            // TODO: There's probably a much better way to handle this. Shouldn't be IPv4 dependent, shouldn't only check first address.
            if (info.getInet4Addresses().length < 1 || info.getInet4Addresses()[0].toString().endsWith(localIP)) continue;
            
            // Remove old versions of the service
            for (Service service : services) {
                if (service.getTitle().equals(info.getName())) {
                    services.remove(service);
                    break;
                }
            }
    	    
    		String title = info.getName();
            String gameType = info.getPropertyString("gameType");
            int gameSize = Integer.parseInt(info.getPropertyString("gameSize"));
            int playerCount = Integer.parseInt(info.getPropertyString("playerCount"));
            Inet4Address[] addresses = info.getInet4Addresses();
            int port = info.getPort();
            
            Service newService = new Service(title, gameType, gameSize, playerCount, addresses, port);
            System.out.println(info);
            System.out.println(newService);
            services.add(newService);
    	}
    	
        runOnUiThread(new Runnable() {
            public void run() {
                drawGames();
            }
        });
    }
    
    private class FindGameListener implements ServiceListener {
        public void serviceAdded(ServiceEvent event) {}
        
        public void serviceRemoved(ServiceEvent event) {
            for (Service service : services) {
                if (service.getTitle().equals(event.getInfo().getName())) {
                    services.remove(service);
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
            ServiceInfo info = event.getInfo();
            //addService(info);
            addServices(jmdns.list("_digitaldeck._tcp.local.")); // Re-list all services when a new one is discovered
        }
    }
    
    /******************* Boring stuff *************************************************************/

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
}
