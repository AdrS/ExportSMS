package com.adrianstoll.exportsms;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Environment;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonWriter;
import android.util.Log;
import android.view.View;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    // TODO: move to utils
    private void showMessage(String msg) {
		Log.d("adrs", msg);
		// TODO: show messages that requires user interaction before going away
		Toast toast = Toast.makeText(
				getApplicationContext(),
				msg,
				Toast.LENGTH_LONG);
		toast.show();
	}
	private boolean isExternalStorageWritable() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}
	private File getExportFile(String name) {
    	// TODO: check that external storage is available
		if(!this.isExternalStorageWritable()) {
			Log.d("adrs", "external storage is not writable");
			return null;
		}
    	File file = new File(Environment.getExternalStoragePublicDirectory(
    			Environment.DIRECTORY_DOWNLOADS),name);
		return file;
	}
	private final static String[] REQUIRED_PERMISSIONS = {
			Manifest.permission.READ_SMS,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	};
    private int numPermissionsGranted = 0;


	@Override
    public void onRequestPermissionsResult(int requestCode,
										   String permissions[],
										   int[] grantResults) {
		for(int i = 0; i < permissions.length; ++i) {
			if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
				Log.d("adrs", permissions[i] + " permission granted");
				this.numPermissionsGranted++;
			} else {
				this.showMessage(permissions[i] + " permission must be granted to export messages");
			}
		}
		Log.d("adrs", "num perms granted: " + Integer.valueOf(this.numPermissionsGranted));
		// Only continue when all request permissions have been granted
		if(this.numPermissionsGranted == REQUIRED_PERMISSIONS.length) {
			this.exportSMS();
		}
	}
	public void dumpToJson(Cursor cur, OutputStreamWriter stream) throws IOException {
		JsonWriter writer = new JsonWriter(stream);

		// Write array of messages
		writer.beginArray();

		// see: https://developer.android.com/reference/android/provider/Telephony.TextBasedSmsColumns#MESSAGE_TYPE_ALL
		// TODO: fetch names from contacts
		while(cur.moveToNext()) {
			// Write message object
			//Log.d("adrs","{");
			writer.beginObject();
			int numCols = cur.getColumnCount();
			for(int i = 0; i < numCols; ++i) {
				String name = cur.getColumnName(i);
				String value = cur.getString(i);
				//Log.d("adrs", "\t" + name + ": " + value);
				writer.name(name).value(value);
			}
			//Log.d("adrs","}");
			writer.endObject();
		}
		cur.close();
		writer.endArray();
		writer.close();
		Log.d("adrs","finished export");
	}
	public void exportSMS() {
		// Make sure we have the proper permissions
		// See what permissions were're missing
		this.numPermissionsGranted = 0;
		ArrayList<String> ungrantedPermissions = new ArrayList<String>();
		for(int i = 0; i < REQUIRED_PERMISSIONS.length; ++i) {
			if(ContextCompat.checkSelfPermission(this,
					REQUIRED_PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
				ungrantedPermissions.add(REQUIRED_PERMISSIONS[i]);
			} else {
				this.numPermissionsGranted++;
			}
		}
		// Ask for ungranted permissions
		if(this.numPermissionsGranted != REQUIRED_PERMISSIONS.length) {
			Log.d("adrs", "asking for permissions");
			ActivityCompat.requestPermissions(this, ungrantedPermissions.toArray(new String[0]), 0);
			Log.d("adrs", "Asked for permissions");
			return;
		}

		// Get a cursor into the SMS message database
		// https://stackoverflow.com/questions/3012287/how-to-read-mms-data-in-android/6446831#6446831 does not get all of them
		// Does not get all messages :(
		//Telephony.MmsSms.CONTENT_CONVERSATIONS_URI,

		// TODO: do for Mms.CONTENT_URI too
		Cursor messageCursor = getContentResolver().query(
				Sms.CONTENT_URI,
				null,
				null,
				null,
				null);

		// Create file to export messages to
		String timestamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
		String exportPath = "SMS-Export-" + timestamp + ".json";
		File exportFile = this.getExportFile(exportPath);
		if(exportFile.exists()) {
			this.showMessage("File " + exportPath + " in downloads already exists. Aborting.");
			return;
		}
		Log.d("adrs", "About to dump");
		// Dump messages to json file
		try {
			// TODO: run in separate thread
			// TODO: show progress bar + cancel button?
			this.dumpToJson(messageCursor, new OutputStreamWriter(new FileOutputStream(exportFile), "UTF-8"));
		} catch (IOException e) {
			this.showMessage("could not export messages: " + e.toString());
			return;
		}
		this.showMessage("Finished Export");
	}
    public void exportHandler(View view) {
		// TODO: export Multimedia SMS + annotate with contact names
        exportSMS();
    }
}
