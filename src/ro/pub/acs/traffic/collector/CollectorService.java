package ro.pub.acs.traffic.collector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ro.pub.acs.macollector.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class CollectorService extends Service 
{

	/** Tag used for debug information */
	private static String TAG = "DEBUG_INFO";
	private File file;
	private BufferedWriter collector;
	
	private LocationManager locationManager;
	private LocationListener locationListener;
	
	private float lastSpeed = -1;
	
	private Database db;
	
	// Handler that receives messages from the thread
	public void onCreate() {
		super.onCreate();
		/* Notify that the service started */
	    showNotification(); 
		/*
		 * GPS stuff
		 */
	    db = new Database(this, "collector", "routes", new String[] { "lat", "long", "speed", "timestamp" });
	    
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				makeUseOfNewLocation(location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}

			public void onProviderEnabled(String provider) {}

			public void onProviderDisabled(String provider) {}
		};
		
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, locationListener);
		
		/*
		 * Storage file & database
		 */
		if(Utils.testCard())
		{
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File (sdCard.getAbsolutePath() + "/collector/");
			dir.mkdirs();
			
			file = new File(dir, "journey.txt");
			FileWriter fw;
			try {
				fw = new FileWriter(file, true);
				collector = new BufferedWriter(fw);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void makeUseOfNewLocation(Location loc)
	{
		try {
			float currentSpeed = loc.getSpeed();
			if(lastSpeed == -1 || 
					(lastSpeed == 0 && currentSpeed != 0) || 
					(lastSpeed != 0 && currentSpeed != 0) || 
					(lastSpeed !=0 && currentSpeed == 0))
			{
				if(Utils.testCard())
				{
					collector.write(loc.getLatitude() + " " + loc.getLongitude() + " " + currentSpeed + " " + loc.getTime());
		        	collector.newLine();
				}
		        db.insert(new String[] { loc.getLatitude() + "", loc.getLongitude() + "", currentSpeed + "", loc.getTime() + "" });
	        	lastSpeed = currentSpeed;
			}
	    } catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	/**
	 * Method executed when the service is stopped
	 */
	@Override
	public void onDestroy() 
	{
		super.onDestroy();
		db.close();
		if(Utils.testCard() && collector != null) {
			try {
				collector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(file.length() == 0)
				file.delete();
		}
		if(locationManager != null && locationListener != null)
			locationManager.removeUpdates(locationListener);
		/* Hide notification */
		hideNotification();
		/* Stop all threads */
		Log.d(TAG, "CommunicationService destroyed");
	}
	
	/**
	 * Method that shows the notification that the service started
	 */
	private void showNotification()
	{
		//NotificationManager lNM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	    int lIcon = R.drawable.icon2;
	    CharSequence lTickerText = "MACollector Service Running";
	    long lWhen = System.currentTimeMillis();
	
	    Notification lNotification = new Notification(lIcon, lTickerText, lWhen);
	    
	    Context lContext = getApplicationContext();
	    CharSequence lContentTitle = "MACollector";
	    CharSequence lContentText = "MACollector Service Running!";
	    Intent lNotificationIntent = new Intent(this, CollectorService.class);
	    PendingIntent lContentIntent = PendingIntent.getActivity(this, 0, lNotificationIntent, 0);
	
	    lNotification.setLatestEventInfo(lContext, lContentTitle, lContentText, lContentIntent);
	    
	    //lNM.notify(1000, lNotification);
	    startForeground(Notification.FLAG_ONGOING_EVENT, lNotification);
	}
	
	/**
	 * Hide notification
	 */
	private void hideNotification()
	{
		//NotificationManager lNM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	    //lNM.cancel(1000);
		stopForeground(true);
	}
	
}