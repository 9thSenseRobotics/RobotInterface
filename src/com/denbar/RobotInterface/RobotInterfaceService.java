package com.denbar.RobotInterface;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.http.util.EncodingUtils;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

public class RobotInterfaceService extends Service implements Runnable {

	// TAG is used to debug in Android logcat console
	private static final String TAG = "ArduinoAccessory";
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	private UsbAccessory mAccessory;
	private ParcelFileDescriptor mFileDescriptor;
	private FileInputStream mInputStream;
	private FileOutputStream mOutputStream;

	private int intentCount;
	public String fromArduino, toArduino;

	@Override
	public void onCreate() {
		super.onCreate();
		intentCount = 0;
		fromArduino = "nothing yet";
		toArduino = "nothing yet";
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
//		if (getLastNonConfigurationInstance() != null) {
//			mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
//			openAccessory(mAccessory);
//		}
	}

/*
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mAccessory != null) {
		return mAccessory;
		} else {
		return super.onRetainNonConfigurationInstance();
		}
	}
*/

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (mInputStream == null || mOutputStream == null) {
			//streams were null
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
				}
			} else {
				// null accessory
		}

		Toast.makeText(this, "in onStart", Toast.LENGTH_SHORT).show();
		ReceivedNewIntent(intent);
		return Service.START_NOT_STICKY;
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
		//mResponseField.setText("Flag: "+t.getFlag()+"; Reading: "+t.getReading()+"; Date: "+(new Date().toString()));
		fromArduino = "Flag: "+t.getFlag()+"; Reading: "+t.getReading()+"; Date: "+(new Date().toString());
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

	public void sendData(String dataToSend) {
		byte[] buffer = EncodingUtils.getAsciiBytes(dataToSend);
		int len = dataToSend.length();

		String message = "sending data, buffer length = " + len + ", value = "+ buffer[0];
		for (int i = 1; i < len; i++) {
			message += ", " + buffer[i];
		}
		Toast.makeText(this, message , Toast.LENGTH_SHORT).show();

		if (mOutputStream != null) {
			try {
				mOutputStream.write(buffer,0,len);
			} catch (IOException e) {
				Toast.makeText(this, "write failed", Toast.LENGTH_SHORT).show();
				Log.e(TAG, "write failed", e);
			}
		}
		else {
			Toast.makeText(this, "mOutputStream is null", Toast.LENGTH_SHORT).show();
		}
	}

	void ReceivedNewIntent(Intent intent) {
		Log.d("YourActivity", "ReceivedNewIntent is called!");

		String command = intent.getStringExtra("command");
		if (command != null) {

			String message = "Robot command intent received = " + command;
			Toast.makeText(this, message , Toast.LENGTH_SHORT).show();

			toArduino = "toArduino = " + command;
			interpretCommand(command);
		}
		else {
			Toast.makeText(this,"command was null" , Toast.LENGTH_SHORT).show();
		}
	}

	void interpretCommand(String command) {
		// parse the command and use known state variables to decide what to do
		// this is where we determine accelerations, stops, etc.
		sendData(command);
	}

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
					//fromArduino = "arduino sent value = " + value;
					m.obj = new ValueMsg('f', value);
					mHandler.sendMessage(m);
				}
				i += 1; // number of bytes sent from arduino
			}
		}
	}



	private final IBinder binder = new MyBinder();

    @Override
    public IBinder onBind(Intent intent) {
    	Log.d(this.getClass().getName(), "RobotInterfaceService running onBind");
    	return binder;
    }

    public class MyBinder extends Binder {
    	RobotInterfaceService getService() {
    		return RobotInterfaceService.this;
    	}
    }

 /*
	private final BroadcastReceiver RobotInterfaceBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String command;
			int val;
			//Uri data = intent.getData();
			//String type = intent.getType();
			command = intent.getStringExtra("command");
			val = intent.getIntExtra("value", 0);
			Toast.makeText(context, "Robot command intent received", Toast.LENGTH_SHORT).show();
			byte testvalue = 0;
			sendData(testvalue);
		}
	};
 */
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
