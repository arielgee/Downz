package com.arielg.downz;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class ActionSendActivity extends Activity {
    
    private static final String TAG = "<<<   ActionSend    >>>";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get incoming intent
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        //Log.d(TAG, "action: " + action);
        //Log.d(TAG, "type: " + type);
        //Log.d(TAG, "intent: " + intent.toString().substring(0, 150));

        if(action == null || type == null || !type.equals("text/plain") ) {
            Log.d(TAG, "Strange intent: " + intent);
            this.onBackPressed();
            return;
        }

        Uri uri = null;
        String textData = null;

        if(action.equals(Intent.ACTION_SEND)) {
            uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

            // if its not a uri maybe its simple text
            if(uri == null) {
                textData = intent.getExtras().getString(Intent.EXTRA_TEXT);
            }
        } else if(action.equals(Intent.ACTION_VIEW)) {
            uri = intent.getData();
        }

        // nothing valid
        if(uri == null && textData == null) {
            Toast.makeText(this, getResources().getString(R.string.error_action_not_supported), Toast.LENGTH_LONG).show();
            this.onBackPressed();
            return;
        }

        // send intent
        intent = new Intent(this, MainActivity.class);

        // send intent
        if(uri != null) {
            intent.setAction(MainActivity.ACTION_INCOMING_FILE_URI);
            intent.putExtra(MainActivity.PARAM_FILE_URI, uri);
            Log.d(TAG, "Incoming Uri: " + uri.getPath());
        } else {        // must be textData!=null
            intent.setAction(MainActivity.ACTION_INCOMING_TEXT_DATA);
            intent.putExtra(MainActivity.PARAM_TEXT_DATA, textData);
            Log.d(TAG, "Incoming Text: " + textData.substring(0, 20));
        }

        startActivity(intent);
    }
}
