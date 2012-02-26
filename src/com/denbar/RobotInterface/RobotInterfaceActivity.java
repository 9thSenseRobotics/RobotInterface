package com.denbar.RobotInterface;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class RobotInterfaceActivity extends Activity implements Runnable {

	// TAG is used to debug in Android logcat console
	private static final String TAG = "ArduinoAccessory";
	private TextView mResponseField;
	private ToggleButton buttonLED;
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	private UsbAccessory mAccessory;
	private ParcelFileDescriptor mFileDescriptor;
	private FileInputStream mInputStream;
	private FileOutputStream mOutputStream;

	private int intentCount;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mResponseField = (TextView)findViewById(R.id.arduinoresponse);
		buttonLED = (ToggleButton) findViewById(R.id.toggleButtonLED);
		intentCount = 0;
		setupAccessory();
	}

	private void setupAccessory() {
		//mUsbManager = UsbManager.getInstance(this);
		//If you are not using the add-on library, you must obtain the UsbManager object in the following manner:
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mPermissionIntent =PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		if (getLastNonConfigurationInstance() != null) {
			mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}
	}


	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mAccessory != null) {
		return mAccessory;
		} else {
		return super.onRetainNonConfigurationInstance();
		}
	}


	@Override
	public void onResume() {
		super.onResume();

		if (mInputStream != null && mOutputStream != null) {
			//streams were not null");
			return;
		}

		//streams were null");
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);

			if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory, mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			// null accessory
		}
	}

	@Override
	public void onPause() {
	super.onPause();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	Handler mHandler = new Handler() {
	@Override
	public void handleMessage(Message msg) {
		ValueMsg t = (ValueMsg) msg.obj;
		// this is where you handle the data you sent. You get it by calling the getReading() function
		mResponseField.setText("Flag: "+t.getFlag()+"; Reading: "+t.getReading()+"; Date: "+(new Date().toString()));
		}
	};



	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, this, "OpenAccessoryTest");
			thread.start();
			//Accessory opened
		} else {
			// failed to open accessory
		}
	}

	private void closeAccessory() {
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

	public void blinkLED(View v){

		byte[] buffer = new byte[1];

		if(buttonLED.isChecked())
			buffer[0]=(byte)0; // button says on, light is off
		else
			buffer[0]=(byte)1; // button says off, light is on

		if (mOutputStream != null) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
	Log.d("YourActivity", "onNewIntent is called!");
	//Toast("new intent received");

	intentCount += 1;

	super.onNewIntent(intent);

	String command;
	int val;
	//Uri data = intent.getData();
	//String type = intent.getType();
	command = intent.getStringExtra("command");
	val = intent.getIntExtra("value", 0);
	//ReceivedData.setText(command);

	byte[] buffer = new byte[1];

	if (intentCount % 2 == 0) buffer[0]=(byte)0; // light turns off
	else 	buffer[0]=(byte)1; // light turns on

	if (mOutputStream != null) {
		try {
			mOutputStream.write(buffer);
		} catch (IOException e) {
			Log.e(TAG, "write failed", e);
		}
	}



	} // End of onNewIntent(Intent intent)

	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (true) { // read data
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;
				if (len >= 1) {
					Message m = Message.obtain(mHandler);
					int value = (int)buffer[i];
					// 'f' is the flag, use for your own logic
					// value is the value from the arduino
					m.obj = new ValueMsg('f', value);
					mHandler.sendMessage(m);
				}
				i += 1; // number of bytes sent from arduino
			}
		}
	}


	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
			synchronized (this) {
				//UsbAccessory accessory = UsbManager.getAccessory(intent);
				// If you are not using the add-on library, you must obtain the UsbAccessory object in the following manner:
				UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					openAccessory(accessory);
				} else {
					// USB permission denied
				}
				mPermissionRequestPending = false;
			}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				//UsbAccessory accessory = UsbManager.getAccessory(intent);
				// If you are not using the add-on library, you must obtain the UsbAccessory object in the following manner:
				UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					//accessory detached
					closeAccessory();
				}
			}
		}
	};

	}