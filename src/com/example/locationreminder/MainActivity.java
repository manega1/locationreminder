package com.example.locationreminder;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	
	EditText et1,et2;
	Button bt,bt2;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		et1=(EditText) findViewById(R.id.num);
		et2=(EditText) findViewById(R.id.per);
		bt= (Button) findViewById(R.id.save);
		bt2= (Button) findViewById(R.id.stop);
		SharedPreferences sf=PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		et1.setText(sf.getString("number", ""));
		et2.setText(sf.getString("percentage", ""));
		
		bt.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
			SharedPreferences sf=PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			Editor et=sf.edit();
			et.putString("number", et1.getText().toString());
			et.putString("percentage", et2.getText().toString());
			et.commit();
			startService(new Intent(MainActivity.this,ServiceLocation.class));
			Toast.makeText(getApplicationContext(),"Details Saved", Toast.LENGTH_LONG).show();
			finish();
			}
		});
		bt2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				stopService(new Intent(MainActivity.this,ServiceLocation.class));
				finish();
			}
		});
	}
@Override
public boolean onOptionsItemSelected(MenuItem item) {
	// TODO Auto-generated method stub
	return super.onOptionsItemSelected(item);
	
}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		menu.add("Setting");
		return true;
	}

}
