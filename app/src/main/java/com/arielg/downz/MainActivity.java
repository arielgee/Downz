package com.arielg.downz;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;


public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "<<<      Main       >>>";
    
    public final static String ACTION_INCOMING_FILE_URI = "com.arielg.downz.action.INCOMING_FILE_URI";
    public final static String PARAM_FILE_URI = "com.arielg.downz.extra.FILE_URI";

    public final static String ACTION_INCOMING_TEXT_DATA = "com.arielg.downz.action.INCOMING_TEXT_DATA";
    public final static String PARAM_TEXT_DATA = "com.arielg.downz.extra.TEXT_DATA";

    private enum ActivityMode {
        INVALID,
        SELECT_LIST_FILE,
        DOWNLOAD_LIST_FILE,
        DOWNLOADING_LIST_FILE,
    }

    private static ActivityMode mActivityMode = ActivityMode.INVALID;

    private boolean mListDownloadedOnce = false;
    private Integer mDownloadedFiles = 0;
    private Integer mDownloadErrors = 0;
    private Integer mCancelledDownloads = 0;

    private final ArrayList<DzFile> mDzFileList = new ArrayList<>();
    private final static Vector<DownloadTask> mDownloadTasks = new Vector<>();

    private SharedPreferences mPrefs = null;

    private TextView mTextCaption = null;
    private ProgressBar mProgressBar = null;
    private TextView mTextDzFileList = null;
    private LinearLayout mLayoutEmpty = null;
    private FloatingActionButton mBtnSelNewListFile = null;
    private FloatingActionButton mBtnStartDownload = null;
    private MenuItem mMnuSelNewListFile = null;
    private MenuItem mMnuStartDownload = null;

    private static Resources mR = null;

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mR = getResources();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mTextCaption = (TextView) findViewById(R.id.txtCaption);
        mProgressBar = (ProgressBar) findViewById(R.id.progressDownloading);
        mTextDzFileList = (TextView) findViewById(R.id.txtFilesList);
        mLayoutEmpty = (LinearLayout) findViewById(R.id.layoutEmpty);
        mBtnSelNewListFile = (FloatingActionButton) findViewById(R.id.btnSelNewListFile);
        mBtnStartDownload = (FloatingActionButton) findViewById(R.id.btnStartDownload);

        mBtnSelNewListFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSelectNewListFile(view.getContext());
            }
        });

        mBtnStartDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDzFileListDownload();
            }
        });

        SettingsActivity.setPreferenceDefaults(this);

        setActivityMode(ActivityMode.SELECT_LIST_FILE);

        verifyStoragePermissions();

        // Handle incoming
        Intent intent = getIntent();
        String action = intent.getAction();

        // no actions, exit
        if (action == null) {
            Log.d(TAG, "onCreate");
            return;
        }

        switch (action) {
            case ACTION_INCOMING_FILE_URI:

                Uri uri = intent.getParcelableExtra(PARAM_FILE_URI);
                handleListFilePath(uri.getPath());
                Log.d(TAG, "onCreate INCOMING_FILE_URI : " + uri.getPath());
                break;

            case ACTION_INCOMING_TEXT_DATA:

                String textData = intent.getStringExtra(PARAM_TEXT_DATA);
                handleTextData(textData);
                Log.d(TAG, "onCreate INCOMING_TEXT_DATA : " + textData.substring(0, 20));
                break;

            default:
                Log.d(TAG, "onCreate ACTION_??? : " + action);
                break;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onResume() {
        super.onResume();

        setActivityMode(mActivityMode);
        showDzFileList();

        Log.d(TAG, "onResume, size: " + ((Integer) mDzFileList.size()).toString());
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        this.moveTaskToBack(true);
        Log.d(TAG, "onBackPressed");
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mMnuSelNewListFile = menu.findItem(R.id.action_select_new_file);
        mMnuStartDownload = menu.findItem(R.id.action_download);

        return true;
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        setActivityMode(mActivityMode);
        return true;
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_select_new_file:
                onSelectNewListFile(this);
                return true;

            case R.id.action_download:
                startDzFileListDownload();
                return true;

            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private void setActivityMode(ActivityMode activityMode) {

        mActivityMode = activityMode;

        if (mDzFileList.size() == 0) {
            mTextCaption.setText(mR.getString(R.string.caption_files));
        } else {
            mTextCaption.setText(mR.getString(R.string.caption_files_params, mDownloadedFiles, mDzFileList.size()));
        }

        mLayoutEmpty.setVisibility(mTextDzFileList.length() == 0 ? View.VISIBLE : View.GONE);

        if (mPrefs.getBoolean(mR.getString(R.string.pref_key_switch_single_action), false)) {
            mBtnSelNewListFile.setImageResource(R.drawable.ic_action_single_action_24dp);
            mBtnSelNewListFile.setBackgroundTintList(ColorStateList.valueOf(mR.getColor(R.color.colorSingleAction, null)));
        } else {
            mBtnSelNewListFile.setImageResource(R.drawable.ic_action_add_24dp);
            mBtnSelNewListFile.setBackgroundTintList(ColorStateList.valueOf(mR.getColor(R.color.colorAccent, null)));
        }

        switch (mActivityMode) {

            case SELECT_LIST_FILE:
                mBtnSelNewListFile.setVisibility(View.VISIBLE);
                mBtnStartDownload.setVisibility(View.GONE);
                if (mMnuSelNewListFile != null) {
                    mMnuSelNewListFile.setEnabled(true);
                    mMnuStartDownload.setEnabled(mDzFileList.size() > 0);
                }
                mProgressBar.setVisibility(View.GONE);
                break;

            case DOWNLOAD_LIST_FILE:
                mBtnSelNewListFile.setVisibility(View.GONE);
                mBtnStartDownload.setVisibility(View.VISIBLE);
                if (mMnuSelNewListFile != null) {
                    mMnuSelNewListFile.setEnabled(true);
                    mMnuStartDownload.setEnabled(true);
                }
                mProgressBar.setVisibility(View.GONE);
                break;

            case DOWNLOADING_LIST_FILE:
                mBtnSelNewListFile.setVisibility(View.GONE);
                mBtnStartDownload.setVisibility(View.GONE);
                if (mMnuSelNewListFile != null) {
                    mMnuSelNewListFile.setEnabled(false);
                    mMnuStartDownload.setEnabled(false);
                }
                mProgressBar.setVisibility(View.VISIBLE);
                break;

            default:
                break;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private boolean isListDownloading() {

        for (int idx = 0; idx < mDzFileList.size(); idx++) {
            if (mDzFileList.get(idx).getStatus() == DzFileStatus.DOWNLOADING) {
                return true;
            }
        }
        return false;
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private void onSelectNewListFile(final Context context) {

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View inputView = inflater.inflate(R.layout.text_input, null);

        final EditText txtInput = (EditText) (inputView.findViewById(R.id.textInput));

        txtInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        txtInput.setHint("file path");
        txtInput.setText(mPrefs.getString(mR.getString(R.string.pref_key_text_recently_used_list_file), ""));
        //txtInput.setText("/sdcard/Download/dzFiles.txt");

        new AlertDialog.Builder(context)
                .setTitle("List File")
                .setView(inputView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        handleListFilePath(txtInput.getText().toString());
                    }
                })
                /*
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {}
                })      */
                .show();
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private void handleListFilePath(String listFilePath) {

        if (!listFilePath.isEmpty() && loadDzFileListFromFile(listFilePath)) {

            // a good file path is saved as a preference
            SettingsActivity.setsPreferenceValue(this, mR.getString(R.string.pref_key_text_recently_used_list_file), listFilePath);

            showDzFileList();

            // [SETTING] single action
            if (mPrefs.getBoolean(mR.getString(R.string.pref_key_switch_single_action), false)) {
                startDzFileListDownload();
            } else {
                setActivityMode(ActivityMode.DOWNLOAD_LIST_FILE);
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private void handleTextData(String textData) {

        if (!textData.isEmpty() && loadDzFileListFromTextData(textData)) {

            showDzFileList();

            // [SETTING] single action
            if (mPrefs.getBoolean(mR.getString(R.string.pref_key_switch_single_action), false)) {
                startDzFileListDownload();
            } else {
                setActivityMode(ActivityMode.DOWNLOAD_LIST_FILE);
            }

        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private boolean loadDzFileListFromFile(String listFilePath) {

        File file = new File(listFilePath);

        BufferedReader br;

        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            Snackbar.make(mTextDzFileList, mR.getString(R.string.error_file_not_found, e.toString()), Snackbar.LENGTH_LONG).show();
            return false;
        }

        mDzFileList.clear();
        mDownloadTasks.clear();

        String line;
        URL url;

        try {

            while ((line = br.readLine()) != null) {
                try {
                    url = new URL(line);
                } catch (MalformedURLException e) {
                    continue;
                }
                mDzFileList.add(new DzFile(url));
            }
            br.close();

        } catch (IOException e) {
            Snackbar.make(mTextDzFileList, mR.getString(R.string.error_reader_failed, e.toString()), Snackbar.LENGTH_LONG).show();
            return false;
        }

        mListDownloadedOnce = false;
        mDownloadTasks.setSize(mDzFileList.size());
        mDownloadedFiles = mDownloadErrors = mCancelledDownloads = 0;

        if (mDzFileList.size() == 0) {
            Snackbar.make(mTextDzFileList, mR.getString(R.string.error_nothing_loaded_from_file), Snackbar.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private boolean loadDzFileListFromTextData(String textData) {

        // for \r & \n; What's between \Q and \E is treated as normal characters, not regexp characters
        final String regExDelimiters = "\\s*(\\Q\r\\E|\\Q\n\\E)\\s*";

        String[] lines = textData.split(regExDelimiters);

        mDzFileList.clear();
        mDownloadTasks.clear();

        URL url;

        for (String line : lines) {
            try {
                url = new URL(line);
            } catch (MalformedURLException e) {
                continue;
            }
            mDzFileList.add(new DzFile(url));
        }

        mListDownloadedOnce = false;
        mDownloadTasks.setSize(mDzFileList.size());
        mDownloadedFiles = mDownloadErrors = mCancelledDownloads = 0;

        if (mDzFileList.size() == 0) {
            Snackbar.make(mTextDzFileList, mR.getString(R.string.error_nothing_loaded_from_text_data), Snackbar.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private void showDzFileList() {

        Log.d(TAG, "showDzFileList IS CALLED");

        // [SETTING] show full url
        // [SETTING] show downloaded percentage
        boolean bShowUrl = mPrefs.getBoolean(mR.getString(R.string.pref_key_switch_show_full_url), true);
        boolean bShowPercentage = mPrefs.getBoolean(mR.getString(R.string.pref_key_switch_show_downloaded_percentage), true);
        StringBuilder list = new StringBuilder("");

        for (int i = 0; i < mDzFileList.size(); i++) {

            list.append(bShowUrl ? mDzFileList.get(i).getPresentableUrl() : mDzFileList.get(i).getPresentableFileName());

            if (bShowPercentage) {
                list.append("  (").append(mDzFileList.get(i).getDownloadedPercentage()).append("%)");
            }
            list.append('\n');
        }

        mTextDzFileList.setText(list.toString());
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private void startDzFileListDownload() {

        // [SETTING] download folder
        File downloadFolder = new File(mPrefs.getString(mR.getString(R.string.pref_key_text_download_folder), SettingsActivity.DEFAULT_DOWNLOAD_FOLDER_NAME));

        try {
            createDownloadDirectory(downloadFolder);
        } catch (IOException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
            return;
        }

        if (mListDownloadedOnce) {
            mDownloadTasks.setSize(mDzFileList.size());
            mDownloadedFiles = mDownloadErrors = mCancelledDownloads = 0;
            for (int idx = 0; idx < mDzFileList.size(); idx++) {
                mDzFileList.get(idx).resetStatus();
            }
        }

        setActivityMode(ActivityMode.DOWNLOADING_LIST_FILE);

        for (int idx = 0; idx < mDzFileList.size(); idx++) {
            mDownloadTasks.set(idx, (DownloadTask) new DownloadTask(this, mDzFileList.get(idx), downloadFolder).execute());
        }
        mListDownloadedOnce = true;
        showDzFileList();
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    public static void cancelDownload() {
        for (DownloadTask task : mDownloadTasks) {
            task.cancel(false);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private void createDownloadDirectory(File downloadFolder) throws IOException {
        if (!downloadFolder.exists() || !downloadFolder.isDirectory()) {
            if (!downloadFolder.mkdirs()) {
                throw new IOException("Fail to create download folder " + downloadFolder.getPath());
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private void verifyStoragePermissions() {

        // Storage Permissions
        final int REQUEST_EXTERNAL_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    @SuppressWarnings("unused")
    private void verifyWakeLockPermissions() {

        // Wake lock Permissions
        final int REQUEST_WAKE_LOCK = 2;
        String[] PERMISSIONS_WAKE_LOCK = {
                Manifest.permission.WAKE_LOCK
        };
        // Check if we have Wake Lock permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_WAKE_LOCK,
                    REQUEST_WAKE_LOCK
            );
        }
    }

    /*####################################################################################################*/
    /*####################################################################################################*/
    /*####################################################################################################*/

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        // usually, subclasses of AsyncTask are declared inside the activity class.
        // that way, you can easily modify the UI thread from here

        private final Context mContext;
        private final DzFile mDzFile;
        private PowerManager.WakeLock mWakeLock;
        private final File mDownloadFolder;

        //////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////
        public DownloadTask(Context context, DzFile dzFile, File downloadFolder) {
            this.mContext = context;
            this.mDzFile = dzFile;
            this.mDownloadFolder = downloadFolder;
        }

        //////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////
        @Override
        protected String doInBackground(String... params) {

            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            try {

                URL url = mDzFile.getUrl();

                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(this.mDownloadFolder + "/" + mDzFile.getFileName());

                byte buffer[] = new byte[4096];
                long total = 0;
                int count;

                while ((count = input.read(buffer)) != -1) {
                    // allow canceling
                    if (isCancelled()) {
                        //noinspection ResultOfMethodCallIgnored
                        new File(this.mDownloadFolder, mDzFile.getFileName()).delete();
                        Log.d(TAG, "delete " + mDzFile.getFileName());
                        return null;
                    }
                    total += count;

                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));

                    output.write(buffer, 0, count);
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: " + e.toString());
                return e.toString();
            } finally {
                try {
                    if (output != null) {
                        output.close();
                    }
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ignored) {
                }

                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }

        //////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) this.mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            mWakeLock.acquire();

            mDzFile.setStatus(DzFileStatus.DOWNLOADING);
            Log.d(TAG, "DownloadTask::onPreExecute " + mDzFile.getFileName());

            //mProgressDialog.show();
        }

        //////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////
        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);

            /*
            update the downloaded percentage value but no need to update the view if the
            settings are set to not display downloaded percentage
             */
            mDzFile.setDownloadedPercentage(progress[0]);

            // [SETTING] show downloaded percentage
            if (mPrefs.getBoolean(mR.getString(R.string.pref_key_switch_show_downloaded_percentage), true)) {
                showDzFileList();
            }

            /*
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);*/
        }

        //////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////
        @Override
        protected void onCancelled(String s) {
            mWakeLock.release();

            mDzFile.setStatus(DzFileStatus.CANCELLED);
            mCancelledDownloads++;

            updateActivityAndNotification();
            Log.d(TAG, "DownloadTask::onCancelled " + mDzFile.getFileName());
        }

        //////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////
        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();

            if (result != null) {
                Toast.makeText(this.mContext, mR.getString(R.string.error_post_file_download, mDzFile.getFileName(), result), Toast.LENGTH_LONG).show();
                mDzFile.setStatus(DzFileStatus.ERROR);
                mDownloadErrors++;

            } else {
                //Toast.makeText(this.mContext, "Downloaded:\n" + mDzFileList.get(mDzFileId).getFileName(), Toast.LENGTH_SHORT).show();
                mDzFile.setStatus(DzFileStatus.FINISHED);
                mDownloadedFiles++;
            }

            updateActivityAndNotification();
            Log.d(TAG, "DownloadTask::onPostExecute " + mDzFile.getFileName());
            
            //mProgressDialog.dismiss();
        }

        //////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////
        private void updateActivityAndNotification() {

            boolean bDownloading = isListDownloading();

            // [SETTING] notifications
            if (mPrefs.getBoolean(mR.getString(R.string.pref_key_switch_notifications), true)) {

                String notifyMsg;

                if (mDownloadErrors == 0 && mCancelledDownloads == 0) {
                    notifyMsg = mR.getString(R.string.notification_msg_params, mDownloadedFiles, mDzFileList.size());
                } else {
                    notifyMsg = mR.getString(R.string.notification_msg_fail_params, mDownloadedFiles, mDzFileList.size(), mDownloadErrors, mCancelledDownloads);
                }
                NotificationService.Notify(mContext, notifyMsg, bDownloading, mDzFileList.size(), mDownloadedFiles + mCancelledDownloads + mDownloadErrors);
            } else {
                NotificationService.Cancel(mContext);
            }

            setActivityMode(bDownloading ? ActivityMode.DOWNLOADING_LIST_FILE : ActivityMode.SELECT_LIST_FILE);
            showDzFileList();
        }
    }
}

