package com.example.locationreminder;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.locationreminder.ServiceLocation.GetLocaPlace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

 public class IncommingCall extends BroadcastReceiver {
    
    public void onReceive(Context context, Intent intent) {
        
    try {
                TelephonyManager tmgr = (TelephonyManager) context
                        .getSystemService(Context.TELEPHONY_SERVICE);
                        
                MyPhoneStateListener PhoneListener = new MyPhoneStateListener(context);
                
                tmgr.listen(PhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        
        } catch (Exception e) {
            Log.e("Phone Receive Error", " " + e);
        }

    }
	class GetLocaPlace extends AsyncTask<String, String, String>
	{
		String setdata="";
		String inum="";
		String level="";
		@Override
		protected String doInBackground(String... arg0) {
			// TODO Auto-generated method stub
			inum=arg0[1];
			level=arg0[2];
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
     	       
			}
			//setdata=url;
			return null;
		}
		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		    
	           SmsManager sms=SmsManager.getDefault();
	            sms.sendTextMessage(inum, null,"Battery Discharge Level "+level +"%"+"Location:"+setdata, null, null);
//	             
		}
	}

    private class MyPhoneStateListener extends PhoneStateListener {

    	Context pContext;
        public MyPhoneStateListener(Context context) {
			// TODO Auto-generated constructor stub
        	pContext=context;
		}

		public void onCallStateChanged(int state, String incomingNumber) {
        
            Log.d("MyPhoneListener",state+"   incoming no:"+incomingNumber);

            double lon=0.0;
            double lat=0.0;
            if (state == 0) {

            			SharedPreferences sf=PreferenceManager.getDefaultSharedPreferences(pContext);
            			String level=sf.getString("level", "10");
            			String per=sf.getString("percentage", "10");
            			GPSTracker gps=new GPSTracker(pContext);
            			if(gps.canGetLocation&&Integer.parseInt(level)<Integer.parseInt(per))
            			{
            				lon=gps.getLongitude();
            				lat=gps.getLatitude();
            				new GetLocaPlace().execute(String.valueOf(lat)+","+String.valueOf(lon)+"&sensor=false",incomingNumber,level);
            			}else
            			{
            				Toast.makeText(pContext, "Can't get location", Toast.LENGTH_LONG).show();
            			}
            			
       	            	      	                
          	         }
          	     };	
     
                
            }
     
}