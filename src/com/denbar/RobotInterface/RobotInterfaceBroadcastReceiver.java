package com.denbar.RobotInterface;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class RobotInterfaceBroadcastReceiver extends BroadcastReceiver {

@Override
public void onReceive(Context context, Intent intent) {
	//Uri data = intent.getData();
	//String type = intent.getType();
	String command = intent.getStringExtra("robotCommand");
	//command = intent.getStringExtra("command");
	//val = intent.getByteExtra("value", (byte)0);
	String message = "Robot command intent received = " + command;
	Toast.makeText(context,message , Toast.LENGTH_SHORT).show();
	UseData(context, command);
}

void UseData(Context context, String command) {
	Intent intent2open = new Intent(context, RobotInterfaceService.class);
	//intent2open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // don't know why this is needed
				// but get a runtime crash without it
	//intent2open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //makes sure the activity doesn't re-open if already open
	intent2open.putExtra("command", command);
	//intent2open.putExtra("value", value);
	context.startService(intent2open);
}

/*
void UseData(Context context, String command) {
	// helper function that creates an intent for UsbCommandService
	// fire an intent
	Intent i = new Intent(context, UsbCommandService.class);
		i.putExtra("command", command);
		context.startService(i);

	}
	*/
}

