package com.example.locationreminder;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class ServiceLocation extends Service{
    private LocationManager locMan;
    private Boolean locationChanged;
    private Handler handler = new Handler();
    
    WakeLocker wl;

    public static Location curLocation;
    public static boolean isService = true;

    LocationListener gpsListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (curLocation == null) {
                curLocation = location;
                locationChanged = true;
            }else if (curLocation.getLatitude() == location.getLatitude() && curLocation.getLongitude() == location.getLongitude()){
                locationChanged = false;
                return;
            }else
                locationChanged = true;

            curLocation = location;

            if (locationChanged)
                locMan.removeUpdates(gpsListener);

        }
        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status,Bundle extras) {
            if (status == 0)// UnAvailable
            {
            } else if (status == 1)// Trying to Connect
            {
            } else if (status == 2) {// Available
            }
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();

        wl.acquire(getApplicationContext());
        curLocation = getBestLocation();

        if (curLocation == null) 
            Toast.makeText(getBaseContext(),"Unable to get your location", Toast.LENGTH_SHORT).show();
        else{
            //Toast.makeText(getBaseContext(), curLocation.toString(), Toast.LENGTH_LONG).show();
        }

        isService =  true;
    }
    final String TAG="LocationService";
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
     return super.onStartCommand(intent, flags, startId);
   }
   @Override
   public void onLowMemory() {
       super.onLowMemory();
   }

   @Override
   public void onStart(Intent i, int startId){
      handler.postDelayed(GpsFinder,1);
   }

   @Override
   public void onDestroy() {
	   wl.release();
       handler.removeCallbacks(GpsFinder);
       handler = null;
       Toast.makeText(this, "Location Based Battery Service Stopped", Toast.LENGTH_SHORT).show();
       isService = false;
   }

   public IBinder onBind(Intent arg0) {
         return null;
  }

  public Runnable GpsFinder = new Runnable(){
    public void run(){

        Location tempLoc = getBestLocation();
        if(tempLoc!=null)
        {
            curLocation = tempLoc;
            BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
   	         public void onReceive(Context context, Intent intent) {
   	             context.unregisterReceiver(this);
   	             int currentLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
   	             int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
   	             int level = -1;
   	             if (currentLevel >= 0 && scale > 0) {
   	                 level = (currentLevel * 100) / scale;
   	             }
   	             SharedPreferences sf=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
   	             String num=sf.getString("number",null);
   	             String per=sf.getString("percentage",null);
   	             Editor et=sf.edit();
   	             et.putString("level", String.valueOf(level));
   	             et.commit();
	             int perc=Integer.parseInt(per);
	             if(level<perc)
	             {
	            	 String param=String.valueOf(curLocation.getLatitude())+","+String.valueOf(curLocation.getLongitude())+"&sensor=false";
	            	 Toast.makeText(getApplicationContext(), "Discharge level reached", Toast.LENGTH_LONG).show();
	            	 ConnectionDetector cd=new ConnectionDetector(getApplicationContext());
	            	 boolean iscon=cd.isConnectingToInternet();
	            	 if(iscon)
	            	 {
	            		 new GetLocaPlace().execute(param); 
	 	 	            
	            	 }
	            	 else
	            	 {
		            	 Toast.makeText(getApplicationContext(), "Internet Connection Failure", Toast.LENGTH_LONG).show();
		            	 String deviceId;
		     			final TelephonyManager mTelephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		     	        if (mTelephony.getDeviceId() != null){
		     	            deviceId = mTelephony.getDeviceId(); 
		     	         }
		     	        else{
		     	            deviceId = Secure.getString(getApplicationContext().getContentResolver(),   Secure.ANDROID_ID); 
		     	         }
	            	    SmsManager sms=SmsManager.getDefault();
	       	            sms.sendTextMessage(num, null,"DeviceId "+deviceId+ " Battery Discharge Critical Level Unknown Location", null, null);
	       	            		 
	            	 }
	            	 stopService(new Intent(getApplicationContext(), ServiceLocation.class));

	             } 
	            else
	            	 Toast.makeText(getApplicationContext(),"Battery Level:"+String.valueOf(level), Toast.LENGTH_SHORT).show();
	             
   	         }
   	     };	
   	     IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
   	     registerReceiver(batteryLevelReceiver, batteryLevelFilter);
   	
         }
        tempLoc = null;
        SharedPreferences sf=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
           String time=sf.getString("time",null);
           
        handler.postDelayed(GpsFinder,10000);// register again to start after 1 seconds...        
    }
  };

	class GetLocaPlace extends AsyncTask<String, String, String>
	{
		String setdata="";
		@Override
		protected String doInBackground(String... arg0) {
			// TODO Auto-generated method stub
			String url="http://maps.googleapis.com/maps/api/geocode/json?latlng=";
			String para="48.283273,14.295041&sensor=false";
			para=arg0[0];
			url=url+para;
			Log.d("", url);
			JSONParser jParser=new JSONParser();
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			JSONObject json = jParser.makeHttpRequest(url, "GET", params);
			try {
				JSONArray jarr=json.getJSONArray("results");
				json=jarr.getJSONObject(0);
				
				setdata=json.getString("formatted_address");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				SharedPreferences sf=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		          String num=sf.getString("number",null);
		          String deviceId;
					final TelephonyManager mTelephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			        if (mTelephony.getDeviceId() != null){
			            deviceId = mTelephony.getDeviceId(); 
			         }
			        else{
			            deviceId = Secure.getString(getApplicationContext().getContentResolver(),   Secure.ANDROID_ID); 
			         }
				SmsManager sms=SmsManager.getDefault();
     	            sms.sendTextMessage(num, null,"DeviceId"+deviceId+ "Battery Discharge Critical Level Unknown Location", null, null);
     	       
			}
			//setdata=url;
			return null;
		}
		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			String deviceId;
			final TelephonyManager mTelephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
	        if (mTelephony.getDeviceId() != null){
	            deviceId = mTelephony.getDeviceId(); 
	         }
	        else{
	            deviceId = Secure.getString(getApplicationContext().getContentResolver(),   Secure.ANDROID_ID); 
	         }
		
			Toast.makeText(getApplicationContext(), setdata, Toast.LENGTH_LONG).show();
			  SharedPreferences sf=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	          String num=sf.getString("number",null);
	            SmsManager sms=SmsManager.getDefault();
	            sms.sendTextMessage(num, null, "DeviceId "+deviceId+" Battery Discharge Critical Level Location:"+setdata, null, null);
	             
		}
	}

    private Location getBestLocation() {
        Location gpslocation = null;
        Location networkLocation = null;

        if(locMan==null){
          locMan = (LocationManager) getApplicationContext() .getSystemService(Context.LOCATION_SERVICE);
        }
        try {
            if(locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000, 1, gpsListener);// here you can set the 2nd argument time interval also that after how much time it will get the gps location
                gpslocation = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            }
            if(locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                locMan.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000, 1, gpsListener);
                networkLocation = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); 
            }
        } catch (IllegalArgumentException e) {
            //Log.e(ErrorCode.ILLEGALARGUMENTERROR, e.toString());
            Log.e("error", e.toString());
        }
        if(gpslocation==null && networkLocation==null)
            return null;

        if(gpslocation!=null && networkLocation!=null){
            if(gpslocation.getTime() < networkLocation.getTime()){
                gpslocation = null;
                return networkLocation;
            }else{
                networkLocation = null;
                return gpslocation;
            }
        }
        if (gpslocation == null) {
            return networkLocation;
        }
        if (networkLocation == null) {
            return gpslocation;
        }
        return null;
    }
}