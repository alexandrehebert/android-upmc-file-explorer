package fr.upmc.ppm.amiralex.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import fr.upmc.ppm.amiralex.R;

public class FileSettingsActivity extends PreferenceActivity {
	
	private ListPreference mod, sort;
	private SharedPreferences prefs;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    	prefs = getSharedPreferences(FileExplorerActivity.PREF_NAME + false, Context.MODE_WORLD_WRITEABLE);
        mod = (ListPreference) getPreferenceScreen().findPreference("mod");
        sort = (ListPreference) getPreferenceScreen().findPreference("sort");
        mod.setValue("" + prefs.getInt(FileExplorerActivity.PREF_KEY_MOD, Integer.parseInt(mod.getValue())));
        sort.setValue("" + prefs.getInt(FileExplorerActivity.PREF_KEY_SORT, Integer.parseInt(mod.getValue())));
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	SharedPreferences.Editor edit = prefs.edit();
    	edit.putInt(FileExplorerActivity.PREF_KEY_MOD, Integer.parseInt(mod.getValue()));
    	edit.putInt(FileExplorerActivity.PREF_KEY_SORT, Integer.parseInt(sort.getValue()));
    	edit.commit();
    }
    
}