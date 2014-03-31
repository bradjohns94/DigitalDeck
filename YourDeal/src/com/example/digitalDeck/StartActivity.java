/**StartActivity
 * @author Bradley Johns
 * The starting screen for the app
 * This activity allows the user to see the title
 * of the app, access the global settings options,
 * and to transition into the SelectGameActivity
 */

package com.example.digitalDeck;

import android.os.Bundle;
import android.app.Activity;
import android.view.*;
import android.content.Intent;

public class StartActivity extends Activity {

    /**onCreate
     * create and display the activity
     */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
        setTitle("Digital Deck");
	}

    /**onCreateOptionsMenu
     * display the action bar
     */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.start, menu);
		return true;
	}

    /**start
     * Transition the app to the SelectGameActivity
     * @param view the view to change from
     */

    public void start(View view) {
        Intent toGameSelect = new Intent(this, FindGameActivity.class);
        startActivity(toGameSelect);
    }

    public void openSettings(View view) {
        Intent toSettings = new Intent(this, SettingsActivity.class);
        startActivity(toSettings);
    }

    public void openHelp(View view) {
    }

    /**onOptionsItemSelected
     * allow the user to open the global settings activity
     * if the settings option is selected
     * @param item the selected item
     * @return if the operation was successful
     */

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent toSettings = new Intent(this, SettingsActivity.class);
            startActivity(toSettings);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
