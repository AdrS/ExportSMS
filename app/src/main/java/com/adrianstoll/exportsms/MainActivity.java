package com.adrianstoll.exportsms;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.Telephony;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.provider.Telephony.Sms;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
	private final static int REQUEST_SMS_READ_PERMISSION = 42;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    // TODO: move to utils
    private void showMessage(String msg) {
		Toast toast = Toast.makeText(
				getApplicationContext(),
				msg,
				Toast.LENGTH_LONG);
		toast.show();
	}
    @Override
    public void onRequestPermissionsResult(int requestCode,
										   String permissions[],
										   int[] grantResults) {
    	// Check that we have an actual result
    	if(requestCode != MainActivity.REQUEST_SMS_READ_PERMISSION &&
				grantResults.length == 0) {
    		return;
		}
		if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
    		Log.d("adrs", "READ_SMS permission granted");
			this.exportSMS();
		} else {
			Log.d("adrs", "READ_SMS permission not granted");
			this.showMessage("READ_SMS permission must be granted to export messages");
		}
	}
	public void exportSMS() {
		Log.d("adrs", "Exporting SMS messages...");
		Log.d("adrs", Telephony.Sms.CONTENT_URI.toString());
		// Check if we have the permission to read SMS messages
		if(ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.READ_SMS},
					MainActivity.REQUEST_SMS_READ_PERMISSION);
			Log.d("adrs", "Asked for permission");
			return;
		}
		Log.d("adrs", "Have permission");
		Cursor messageCursor = getContentResolver().query(
				Telephony.Sms.CONTENT_URI,
				null,
				null,
				null,
				null);
		int max = 25;
		int cur = 0;
		while(messageCursor.moveToNext() && cur < max) {
			int numCols = messageCursor.getColumnCount();
			for(int i = 0; i < numCols; ++i) {
				Log.d("adrs",
						messageCursor.getColumnName(i) + ": " + messageCursor.getString(i));
			}
			++cur;
		}
		messageCursor.close();
	}
    public void exportHandler(View view) {
        exportSMS();
    }
}
