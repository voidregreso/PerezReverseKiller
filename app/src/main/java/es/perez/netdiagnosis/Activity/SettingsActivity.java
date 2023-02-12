package es.perez.netdiagnosis.Activity;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.security.KeyChain;
import androidx.appcompat.app.ActionBar;
import android.view.MenuItem;
import android.widget.Toast;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.proxy.dns.AdvancedHostResolver;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;

import com.perez.revkiller.R;
import com.perez.catchexception.CrashApp;
import es.perez.netdiagnosis.Utils.DeviceUtils;
import es.perez.netdiagnosis.Utils.FileUtil;
import es.perez.netdiagnosis.Utils.SharedPreferenceUtils;
import es.perez.netdiagnosis.View.LoadingDialog;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity implements Preference.OnPreferenceChangeListener {

    ListPreference lp;
    Preference hostPreference;

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        addPreferencesFromResource(R.xml.pref_data_sync);

        lp = (ListPreference) findPreference("select_ua");
        
        lp.setOnPreferenceChangeListener(this);
        lp.setSummary(lp.getEntry());

        findPreference("system_host").setOnPreferenceChangeListener(this);

        findPreference("enable_filter").setOnPreferenceChangeListener(this);

        findPreference("install_cert").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                changeSystemProxy();
                return false;
            }
        });

        findPreference("system_proxy").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                return false;
            }
        });

        hostPreference = findPreference("app_host");
        hostPreference.setSummary(getHost());
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }


    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof ListPreference) {
            
            ListPreference listPreference = (ListPreference) preference;
            
            CharSequence[] entries = listPreference.getEntries();
            
            int index = listPreference.findIndexOfValue((String) newValue);
            
            listPreference.setSummary(entries[index]);
        }

        if (preference.getKey().equals("system_host")) {
            DeviceUtils.changeHost(((CrashApp)getApplication()).proxy,newValue.toString());
            hostPreference.setSummary(getHost());
        }

        
        if (preference.getKey().equals("enable_filter")) {
            Toast.makeText(this, "The function will take effect after restarting the program", Toast.LENGTH_SHORT).show();
        }
        return true;
    }



    public void installCert() {
        final String CERTIFICATE_RESOURCE = Environment.getExternalStorageDirectory() + "/har/littleproxy-mitm.pem";
        Toast.makeText(this, "A certificate must be installed to achieve HTTPS packet capture", Toast.LENGTH_LONG).show();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] keychainBytes;
                    FileInputStream is = null;
                    try {
                        is = new FileInputStream(CERTIFICATE_RESOURCE);
                        keychainBytes = new byte[is.available()];
                        is.read(keychainBytes);
                    } finally {
                        IOUtils.closeQuietly(is);
                    }

                    Intent intent = KeyChain.createInstallIntent();
                    intent.putExtra(KeyChain.EXTRA_CERTIFICATE, keychainBytes);
                    intent.putExtra(KeyChain.EXTRA_NAME, "NetworkDiagnosis CA Certificate");
                    startActivityForResult(intent, 3);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        FileUtil.checkPermission(this,runnable);
    }

    private LoadingDialog loadingDialog;

    public void showLoading(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (loadingDialog == null) {
                        loadingDialog = new LoadingDialog(SettingsActivity.this, text);
                    }
                    loadingDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void dismissLoading() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loadingDialog != null) {
                    loadingDialog.dismiss();
                    loadingDialog = null;
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 3) {
            if (resultCode == Activity.RESULT_OK) {
                SharedPreferenceUtils.putBoolean(this, "isInstallNewCert", true);
                Toast.makeText(this, "Successful installation", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to install", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void changeSystemProxy() {
        installCert();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public String getHost() {
        String result = "";
        BrowserMobProxy browserMobProxy = ((CrashApp) getApplication()).proxy;
        AdvancedHostResolver advancedHostResolver = browserMobProxy.getHostNameResolver();
        for (String key : advancedHostResolver.getHostRemappings().keySet()) {
            result += key + " " + advancedHostResolver.getHostRemappings().get(key) + "\n";
        }
        return result.length() > 1 ? result.substring(0, result.length() - 1) : "None";
    }
}
