package com.arielg.downz;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.MenuItem;

import java.io.File;

public class SettingsActivity extends PreferenceActivity {
    
    private static final String TAG = "<<<    Settings     >>>";
    
    public final static String DEFAULT_DOWNLOAD_FOLDER_NAME = "Downz";

    private AppCompatDelegate mDelegate;

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private final static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference pref, Object value) {

            Log.d(TAG,"onPreferenceChange - key:" + pref.getKey() + ", value:" + value);

            if(pref instanceof SwitchPreference) {

                Boolean bVal = (Boolean)value;
                Resources res = pref.getContext().getResources();

                if(pref.getKey().equals(res.getString(R.string.pref_key_switch_single_action))) {
                    pref.setSummary(bVal ? res.getString(R.string.pref_summery_switch_single_action_true) : res.getString(R.string.pref_summery_switch_single_action_false));
                } else if(pref.getKey().equals(res.getString(R.string.pref_key_switch_show_full_url))) {
                    pref.setSummary(bVal ? res.getString(R.string.pref_summery_switch_show_full_url_true) : res.getString(R.string.pref_summery_switch_show_full_url_false));
                } else if(pref.getKey().equals(res.getString(R.string.pref_key_switch_show_downloaded_percentage))) {
                    pref.setSummary(bVal ? res.getString(R.string.pref_summery_switch_show_downloaded_percentage_true) : res.getString(R.string.pref_summery_switch_show_downloaded_percentage_false));
                } else if(pref.getKey().equals(res.getString(R.string.pref_key_switch_notifications))) {
                    pref.setSummary(bVal ? res.getString(R.string.pref_summery_switch_notifications_true) : res.getString(R.string.pref_summery_switch_notifications_false));
                }

            } else {
                String sVal = value.toString();
                pref.setSummary(sVal.isEmpty() ? "<not set>" : sVal);
            }
            return true;
        }
    };

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's current value.
        if(preference instanceof SwitchPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), false));
        } else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getDelegate().getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //NavUtils.navigateUpFromSameTask(this);
            Intent intent = NavUtils.getParentActivityIntent(this);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            NavUtils.navigateUpTo(this, intent);
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    public static void setsPreferenceValue(Context context, String keyPref, String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(keyPref, value);
        editor.apply();
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    public static void setPreferenceDefaults(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String keyDownloadFolder = context.getResources().getString(R.string.pref_key_text_download_folder);

        if(prefs.getString(keyDownloadFolder, null) == null) {

            String sFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString(), SettingsActivity.DEFAULT_DOWNLOAD_FOLDER_NAME).getPath();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(keyDownloadFolder, sFolder);
            editor.apply();
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////
    private static void resetPreferenceToDefaults(Context context) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.clear();
        editor.apply();
    }

    /*####################################################################################################*/
    /*####################################################################################################*/
    /*####################################################################################################*/

    public static class SettingsFragment extends PreferenceFragment {

        //////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);     // Load the preferences from an XML resource
            setHasOptionsMenu(true);

            Preference button = findPreference(getResources().getString(R.string.pref_key_button_reset_settings));
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    new AlertDialog.Builder(getContext())
                            .setTitle("Confirm")
                            .setMessage("Are you sure?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    resetPreferenceToDefaults(getContext());
                                    setPreferenceDefaults(getContext());
                                    ((SettingsActivity)getContext()).recreate();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {}
                            })
                            .show();

                    return true;
                }
            });

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_switch_single_action)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_switch_show_full_url)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_switch_show_downloaded_percentage)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_switch_notifications)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_text_recently_used_list_file)));
            bindPreferenceSummaryToValue(findPreference(getResources().getString(R.string.pref_key_text_download_folder)));
        }

        //////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), MainActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}