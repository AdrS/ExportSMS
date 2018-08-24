package com.adrianstoll.exportsms;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
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
		if (!this.isExternalStorageWritable()) {
			return null;
		}
		File file = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_DOWNLOADS), name);
		return file;
	}

	private final static String[] REQUIRED_PERMISSIONS = {
			Manifest.permission.READ_SMS,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	};
	private int numPermissionsGranted = 0;
	private final static int SMS_EXPORT = 0;
	private final static int MMS_EXPORT = 1;

	@Override
	public void onRequestPermissionsResult(int requestType,
										   String permissions[],
										   int[] grantResults) {
		for (int i = 0; i < permissions.length; ++i) {
			if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
				this.numPermissionsGranted++;
			} else {
				this.showMessage(permissions[i] + " permission must be granted to export messages");
			}
		}
		// Only continue when all request permissions have been granted
		if (this.numPermissionsGranted == REQUIRED_PERMISSIONS.length) {
			this.export(requestType);
		}
	}

	public void dumpToJson(Cursor cur, OutputStreamWriter stream) throws IOException {
		JsonWriter writer = new JsonWriter(stream);

		// Write array of messages
		writer.beginArray();
		while (cur.moveToNext()) {
			// Write message object
			writer.beginObject();
			int numCols = cur.getColumnCount();
			for (int i = 0; i < numCols; ++i) {
				String name = cur.getColumnName(i);
				String value = cur.getString(i);
				writer.name(name).value(value);
			}
			writer.endObject();
		}
		writer.endArray();
		writer.close();
	}

	// Asks user for any required ungranted permissions
	// Returns true if app already has all required permissions
	private boolean aquirePermissions(int requestCode) {
		// See what permissions were're missing
		this.numPermissionsGranted = 0;
		ArrayList<String> ungrantedPermissions = new ArrayList<String>();
		for (int i = 0; i < REQUIRED_PERMISSIONS.length; ++i) {
			if (ContextCompat.checkSelfPermission(this,
					REQUIRED_PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
				ungrantedPermissions.add(REQUIRED_PERMISSIONS[i]);
			} else {
				this.numPermissionsGranted++;
			}
		}
		// Ask for ungranted permissions
		if (this.numPermissionsGranted != REQUIRED_PERMISSIONS.length) {
			Log.d("adrs", "asking for permissions");
			ActivityCompat.requestPermissions(this, ungrantedPermissions.toArray(new String[0]), requestCode);
			Log.d("adrs", "Asked for permissions");
			return false;
		}
		return true;
	}

	/*
	public void dumpContentResolver(ContentResolver content, OutputStream output) {

	}*/

	public void export(int exportType) {
		// Make sure we have the proper permissions
		if (!this.aquirePermissions(exportType)) {
			return;
		}
		// Get a cursor into the message database
		Uri contentUri = exportType == MainActivity.MMS_EXPORT ? Mms.CONTENT_URI : Sms.CONTENT_URI;
		// https://stackoverflow.com/questions/3012287/how-to-read-mms-data-in-android/6446831#6446831 does not get all of them
		Cursor messageCursor = getContentResolver().query(
				contentUri,
				null,
				null,
				null,
				null);

		// Create file to export messages to
		String prefix = exportType == MainActivity.MMS_EXPORT ? "MMS" : "SMS";
		String timestamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
		String exportPath = prefix + "-Export-" + timestamp + ".json";
		File exportFile = this.getExportFile(exportPath);
		/*if (exportFile.exists()) {
			this.showMessage("File " + exportPath + " in downloads already exists. Aborting.");
			return;
		}*/
		Log.d("adrs", "About to dump");
		// Dump messages to json file
		try {
			// TODO: run in separate thread
			// TODO: show progress bar + cancel button?
			this.dumpToJson(messageCursor, new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(exportFile)), "UTF-8"));
			messageCursor.close();
		} catch (IOException e) {
			this.showMessage("could not export messages: " + e.toString());
			return;
		}
		// Export MMS data
		if(exportType == MainActivity.MMS_EXPORT) {
			Log.d("adrs", "About to dump MMS parts");
			Cursor dataCursor = getContentResolver().query(
					Uri.parse("content://mms/part"),
					null,
					null,
					null,
					null);

			String dataPath = prefix + "-Parts-Export-" + timestamp + ".json";
			exportFile = this.getExportFile(dataPath);
			try {
				// TODO: run in separate thread
				// TODO: show progress bar + cancel button?
				this.dumpToJson(dataCursor, new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(exportFile)), "UTF-8"));
				dataCursor.close();
			} catch (IOException e) {
				this.showMessage("could not export MMS parts: " + e.toString());
				return;
			}
		}
		this.showMessage("Finished Export");
	}

	public void exportSmsHandler(View view) {
		// TODO: export Multimedia SMS + annotate with contact names
		// TODO: check that all messages are being exported
		// TODO: create an ICON
		export(MainActivity.SMS_EXPORT);
	}

	public void exportMmsHandler(View view) {
		export(MainActivity.MMS_EXPORT);
	}
}
