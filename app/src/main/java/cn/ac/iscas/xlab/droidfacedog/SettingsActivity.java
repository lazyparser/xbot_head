package cn.ac.iscas.xlab.droidfacedog;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

import cn.ac.iscas.xlab.droidfacedog.config.Config;
import cn.ac.iscas.xlab.droidfacedog.util.RegexCheckUtil;


public class SettingsActivity extends AppCompatPreferenceActivity {
    public static Resources res;

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            String key = preference.getKey();
            if (key.equals(res.getString(R.string.key_recognition_server_ip))) {
                String stringValue = value.toString();
                //先保存上次设置的合法值
                String oldSummary = (String) ( preference).getSummary();
                if (RegexCheckUtil.isRightIP(stringValue)) {
                    Config.RECOGNITION_SERVER_IP = stringValue;
                    preference.setSummary(stringValue);
                }else{
                    preference.setSummary(oldSummary);
                    Toast.makeText(preference.getContext(),res.getString(R.string.toast_wrong_ip), Toast.LENGTH_SHORT).show();
                    return false;
                }
            }else if(key.equals(res.getString(R.string.key_ros_server_ip))){
                String stringValue = value.toString();
                String oldSummary = (String) ( preference).getSummary();
                if (RegexCheckUtil.isRightIP(stringValue)) {
                    Config.ROS_SERVER_IP = stringValue;
                    preference.setSummary(stringValue);
                }else{
                    preference.setSummary(oldSummary);
                    Toast.makeText(preference.getContext(),res.getString(R.string.toast_wrong_ip), Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else if (key.equals(res.getString(R.string.key_recog_threshold))) {
                String stringValue = value.toString();
                if (!stringValue.equals(preference.getSummary())) {
                    double d = Double.parseDouble(stringValue) / 100.0;
                    preference.setSummary(String.valueOf(d));
                    Config.RECOG_THRESHOLD = d;
                }
            } else if (key.equals(res.getString(R.string.key_enable_notification))) {
                Config.ENABLE_MESSAGE_NOTIFICATION = (Boolean) value;
            }
            Log.i("tag", Config.string());

            return true;
        }
    };


    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        if(preference instanceof EditTextPreference){
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        } else if (preference instanceof SwitchPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(),true)
            );
        } else if (preference instanceof SeekbarPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getInt(preference.getKey(), 60)
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        res = getResources();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            int SUCCESS_RESULT=1;
            setResult(SUCCESS_RESULT, new Intent());
            finish();  //return to caller
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }


    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }


    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.key_recognition_server_ip)));
            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.key_ros_server_ip)));
            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.key_recog_threshold)));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(res.getString(R.string.key_enable_notification )));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

}
