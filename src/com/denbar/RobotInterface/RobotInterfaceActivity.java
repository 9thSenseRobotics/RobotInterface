package com.denbar.RobotInterface;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class RobotInterfaceActivity extends Activity { // implements Runnable {
	private TextView mResponseField;
	private ToggleButton buttonLED;
	private RobotInterfaceService serviceBinder;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mResponseField = (TextView)findViewById(R.id.arduinoresponse);
		buttonLED = (ToggleButton) findViewById(R.id.toggleButtonLED);

		startService(new Intent(this,RobotInterfaceService.class));

		Intent bindIntent = new Intent(this, RobotInterfaceService.class);
		bindService(bindIntent, mConnection, Context.BIND_AUTO_CREATE);
	}

	// Handles the connection between RobotInterfaceService and this activity
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// called when the connection is made
			serviceBinder = ((RobotInterfaceService.MyBinder)service).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			// Received when the service unexpectedly disconnects
			serviceBinder = null;
		}
	};

	public void blinkLED(View v){

		byte testdata;
		if(buttonLED.isChecked())
			testdata='f'; // button says on, light is off
		else
			testdata='b'; // button says off, light is on

		serviceBinder.sendData(testdata);

		String messages = "To arduino" + serviceBinder.toArduino + ", from Arduino = " + serviceBinder.fromArduino;
		mResponseField.setText(messages);

	}

}




