package com.denbar.RobotInterface;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class RobotInterfaceBroadcastReceiver extends BroadcastReceiver {

@Override
public void onReceive(Context context, Intent intent) {
	String command;
	int val;
	//Uri data = intent.getData();
	//String type = intent.getType();
	command = intent.getStringExtra("command");
	val = intent.getIntExtra("value", 0);
	Toast.makeText(context, "Robot command intent received", Toast.LENGTH_SHORT).show();
	UseData(context, command, val);
}

void UseData(Context context, String command, int value) {
	Intent intent2open = new Intent(context, RobotInterfaceActivity.class);
	intent2open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // don't know why this is needed
				// but get a runtime crash without it
	//intent2open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); //makes sure the activity doesn't re-open if already open
	intent2open.putExtra("command", command);
	intent2open.putExtra("value", value);
	context.startActivity(intent2open);
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

