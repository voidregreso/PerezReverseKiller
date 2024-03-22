package com.perez.netdiagnosis.Activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadStatusDelegate;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarPage;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.perez.netdiagnosis.Adapter.PageFilterAdapter;
import com.perez.netdiagnosis.Bean.PageBean;
import com.perez.netdiagnosis.Fragment.BaseFragment;
import com.perez.netdiagnosis.Fragment.BackHandledInterface;
import com.perez.netdiagnosis.Fragment.NetworkFragment;
import com.perez.netdiagnosis.Fragment.PreviewFragment;
import com.perez.netdiagnosis.Fragment.WebViewFragment;
import com.perez.revkiller.R;
import com.perez.catchexception.CrashApp;

import com.perez.netdiagnosis.Utils.DeviceUtils;
import com.perez.netdiagnosis.Utils.FileUtil;
import com.perez.netdiagnosis.Utils.SharedPreferenceUtils;
import com.perez.netdiagnosis.Utils.ZipUtils;
import com.perez.netdiagnosis.View.LoadingDialog;
import com.perez.revkiller.databinding.ActivityHttpcapmainBinding;

/**
 * Created by xuzhou on 2016/8/10.
 * NDGAct
 */
public class NDGAct extends AppCompatActivity implements BackHandledInterface {
    private ActivityHttpcapmainBinding binding;
    public final static String CODE_URL = "#";
    public final static String UPLOAD_URL = "#";
    public final static String HOME_URL = "https://github.com";
    public final static String GUIDE_URL = "https://github.com";

    public final static int TYPE_NONE = 0;
    public final static int TYPE_SHARE = 1;
    public final static int TYPE_UPLOAD = 2;

    private int mLastHeightOfContainer; 
    private int mHeightOfVisibility;
    Boolean isKeyboardOpen = false;
    Boolean shouldExitSearchView = false;

    private BaseFragment mBackHandedFragment;
    private long exitTime = 0;

    private Receiver receiver;

    public SearchView searchView;
    public MenuItem homeItem;
    public MenuItem searchItem;
    public MenuItem filterMenuItem;

    public Set<String> disablePages = new HashSet<>();
    public StringBuffer consoleLog = new StringBuffer();

    public SharedPreferences shp;

    public ActivityHttpcapmainBinding getBinding() {
        return binding;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        binding = ActivityHttpcapmainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initFloatingActionMenu();

        OnGlobalLayoutListener globalLayoutListener = new OnGlobalLayoutListener(binding.flContain);
        binding.flContain.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);

        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        if (getIntent().getStringExtra("url") != null && getIntent().getStringExtra("url").length() > 0) {
            WebViewFragment webViewFragment = WebViewFragment.getInstance();
            webViewFragment.loadUrl(getIntent().getStringExtra("url"));
            switchContent(webViewFragment);
        } else {
            switchContent(WebViewFragment.getInstance());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(navigationItemListener);
        binding.navView.getMenu().getItem(0).setChecked(true);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (binding.fab.isOpened()) {
            binding.fab.close(true);
        } else if (mBackHandedFragment == null || !(mBackHandedFragment instanceof WebViewFragment)) {
            switchContent(WebViewFragment.getInstance());
        } else if (!mBackHandedFragment.onBackPressed()) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "Press it again to exit the program", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.httpcap_main, menu);

        filterMenuItem = menu.findItem(R.id.action_filter);
        homeItem = menu.findItem(R.id.action_home);
        searchItem = menu.findItem(R.id.search);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setFocusable(false);
        searchView.setIconifiedByDefault(true);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint("Please input URL keywords...");

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchContent(PreviewFragment.getInstance());
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                PreviewFragment.getInstance().filterItem(query);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                PreviewFragment.getInstance().filterItem(newText);
                shouldExitSearchView = newText.length() == 0;
                return false;
            }
        });
        return true;
    }

    public void changeStateBar(Fragment fragment) {
        if (filterMenuItem != null && searchItem != null && homeItem != null) {
            if (fragment instanceof WebViewFragment) {
                homeItem.setVisible(true);
            } else {
                homeItem.setVisible(false);
            }
            if (fragment instanceof PreviewFragment) {
                filterMenuItem.setVisible(true);
                searchItem.setVisible(true);
                binding.fabClear.setVisibility(View.VISIBLE);
            } else {
                filterMenuItem.setVisible(false);
                searchItem.setVisible(false);
                binding.fabClear.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_home){
            WebViewFragment webViewFragment = WebViewFragment.getInstance();
            webViewFragment.loadUrl(HOME_URL);
            switchContent(webViewFragment);
            return true;
        }
        if(id == R.id.action_guide){
            WebViewFragment webViewFragment = WebViewFragment.getInstance();
            webViewFragment.loadUrl(GUIDE_URL);
            switchContent(webViewFragment);
            return true;
        }

        if (id == R.id.action_filter) {
            showFilter(this, TYPE_NONE);
            return true;
        }
        if (id == R.id.action_exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public NavigationView.OnNavigationItemSelectedListener navigationItemListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            // Handle navigation view item clicks here.
            int id = item.getItemId();

            if (!CrashApp.isInitProxy) {
                Toast.makeText(NDGAct.this, "Please wait until the program completes initialization", Toast.LENGTH_LONG).show();
                return true;
            }

            switch (id) {
                case R.id.nav_gallery:
                    switchContent(WebViewFragment.getInstance());
                    break;
                case R.id.nav_preview:
                    switchContent(PreviewFragment.getInstance());
                    break;
                case R.id.nav_slideshow:
                    switchContent(NetworkFragment.getInstance());
                    break;
                case R.id.nav_manage: {
                    Intent intent = new Intent(NDGAct.this, SettingsActivity.class);
                    startActivity(intent);
                    break;
                }
                case R.id.nav_ua:
                    showUaDialog();
                    break;
                case R.id.nav_modify:
                    if (shp.getBoolean("enable_filter", false)) {
                        Intent intent = new Intent(NDGAct.this, ChangeFilterActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(NDGAct.this, "Please go to settings to enable injection function", Toast.LENGTH_LONG).show();
                    }
                    break;
                case R.id.nav_cosole:
                    showLogDialog();
                    break;
                case R.id.nav_host:
                    showHostDialog();
                    break;
            }

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }
    };

    public void switchContent(Fragment to) {
        Boolean isAdded = false;
        try {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (getSupportFragmentManager().getFragments() != null) {
                for (Fragment f : getSupportFragmentManager().getFragments()) {
                    if (to.getClass().isAssignableFrom(f.getClass())) {
                        if (!f.isAdded()) {
                            transaction.add(R.id.fl_contain, f, f.getClass().getName());
                        } else {
                            transaction.show(f);
                        }
                        isAdded = true;
                    } else {
                        transaction.hide(f);
                        f.setUserVisibleHint(false);
                    }
                }
            }
            if (!isAdded) {
                if (!to.isAdded()) {
                    transaction.add(R.id.fl_contain, to, to.getClass().getName()).commitNow();
                } else {
                    transaction.show(to).commitNow(); 
                }
            } else {
                transaction.commitNow();
            }
            if (getSupportFragmentManager().findFragmentByTag(to.getClass().getName()) != null) {
                getSupportFragmentManager().findFragmentByTag(to.getClass().getName()).setUserVisibleHint(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setSelectedFragment(BaseFragment selectedFragment) {
        this.mBackHandedFragment = selectedFragment;
    }

    public void installCert() {
        final String CERTIFICATE_RESOURCE = Environment.getExternalStorageDirectory() + "/har/littleproxy-mitm.pem";
        Boolean isInstallCert = SharedPreferenceUtils.getBoolean(this, "isInstallNewCert", false);

        Runnable runnable = () -> {
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
        };

        if (!isInstallCert) {
            Toast.makeText(this, R.string.mustinstcert, Toast.LENGTH_LONG).show();
            FileUtil.checkPermission(this,runnable);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 3) {
            if (resultCode == Activity.RESULT_OK) {
                SharedPreferenceUtils.putBoolean(this,"isInstallNewCert", true);
                Toast.makeText(this, "Installation successful", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(this, "Installation fail", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Boolean shouldDispatchTouchEvent = false;
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideInput(v, ev)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null && v != null) {
                    if (isKeyboardOpen) {
                        shouldDispatchTouchEvent = true;
                    }
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
            return shouldDispatchTouchEvent || super.dispatchTouchEvent(ev);
        }
        return getWindow().superDispatchTouchEvent(ev) || onTouchEvent(ev);
    }

    public boolean isShouldHideInput(View view, MotionEvent event) {
        if (view != null && (view instanceof EditText)) {
            int[] leftTop = {0, 0};
            view.getLocationInWindow(leftTop);
            
            int left = leftTop[0];
            int top = leftTop[1];
            int bottom = top + view.getHeight();
            int right = left + view.getWidth();
            if (event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom) {
                return false;
            }
        }

        return true;
    }

    private class OnGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        private View mView;

        public OnGlobalLayoutListener(View view) {
            mView = view;
        }

        @Override
        public void onGlobalLayout() {
            int currentHeight = mView.getHeight();
            if (currentHeight < mLastHeightOfContainer) { 
                if (mHeightOfVisibility == 0) {
                    mHeightOfVisibility = currentHeight;
                }
                isKeyboardOpen = true;
            } else if (currentHeight > mLastHeightOfContainer && mLastHeightOfContainer != 0) { 
                isKeyboardOpen = false;
                
                if (shouldExitSearchView) {
                    searchItem.collapseActionView();
                }
            }
            mLastHeightOfContainer = currentHeight;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try {
            setIntent(intent);
            handleUriStartupParams();
            if (intent.getAction().equals("android.intent.action.SEARCH")) {
                switchContent(PreviewFragment.getInstance());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        receiver = new Receiver();
        registerReceiver(receiver, new IntentFilter("proxyfinished"));
        handleUriStartupParams();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(receiver);
        super.onStop();
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            installCert();
            Log.i("~~~~", "Receiver installCert");
        }
    }

    private void handleUriStartupParams() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        Uri uri = intent.getData();
        if (uri == null) {
            return;
        }
        intent.setData(null);
        String query = uri.getQuery();
        if (query == null || query.length() < 8) {
            return;
        }
        try {
            String jsonString = query.substring(6);
            JSONObject json = new JSONObject(jsonString);

            WebViewFragment webViewFragment = WebViewFragment.getInstance();
            webViewFragment.loadUrl(json.getString("url"));

            switchContent(webViewFragment);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void createZip(final Runnable callback) {
        Runnable runnable = () -> new Thread(() -> {
            try {
                showLoading("Packing...");

                final Har har = getFiltedHar();
                final File saveHarFile = new File(Environment.getExternalStorageDirectory() + "/har/test.har");
                har.writeTo(saveHarFile);

                ZipUtils.zip(Environment.getExternalStorageDirectory() + "/har",
                        Environment.getExternalStorageDirectory() + "/test.zip");

                binding.flContain.post(new Runnable() {
                    @Override
                    public void run() {
                        Snackbar.make(binding.flContain, "HAR file has been saved to " + saveHarFile.getPath() + " Totally there are"
                                + har.getLog().getEntries().size() + " requests.", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                });

                NDGAct.this.runOnUiThread(callback);
            } catch (Exception e) {
                binding.flContain.post(new Runnable() {
                    @Override
                    public void run() {
                        Snackbar.make(binding.flContain, "HAR file failed to save", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    }
                });
                e.printStackTrace();
            } finally {
                dismissLoading();
            }
        }).start();

        FileUtil.checkPermission(this,runnable);
    }

    public void shareZip() {
        Runnable runnable = () -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.sharehar));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/test.zip")));
            startActivity(Intent.createChooser(intent, "share"));
        };

        createZip(runnable);
    }

    public void uploadZip() {
        showUploadDialog(this);
    }

    public class MyUploadDelegate implements UploadStatusDelegate {
        @Override
        public void onProgress(Context context, UploadInfo uploadInfo) {
            Log.e("~~~~", uploadInfo.getProgressPercent() + "");
        }

        @Override
        public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Exception exception) {
            dismissLoading();
            Snackbar.make(binding.flContain, "Failed to upload!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            exception.printStackTrace();
        }

        @Override
        public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
            try {
                JSONObject jsonObject = new JSONObject(serverResponse.getBodyAsString());
                if (jsonObject.getInt("errId") == 0) {
                    Snackbar.make(binding.flContain, "Succeeded in uploading", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                } else if (jsonObject.getInt("errId") == 2 || jsonObject.getInt("errId") == 11004) {
                    Snackbar.make(binding.flContain, "Verification code error!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    showUploadDialog(NDGAct.this);
                } else {
                    Snackbar.make(binding.flContain, "Failed to upload!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            } catch (Exception e) {
                Snackbar.make(binding.flContain, "Failed to upload!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
            dismissLoading();
        }

        @Override
        public void onCancelled(Context context, UploadInfo uploadInfo) {
            dismissLoading();
        }
    }

    private LoadingDialog loadingDialog;

    public void showLoading(String text) {
        try {
            if (loadingDialog == null) {
                loadingDialog = new LoadingDialog(this, text);
            }
            loadingDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dismissLoading() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    private void showUploadDialog(Context context) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View textEntryView = inflater.inflate(R.layout.alert_code, null);
        final EditText edtInput = (EditText) textEntryView.findViewById(R.id.et_code);
        final ImageView imageView = (ImageView) textEntryView.findViewById(R.id.iv_code);
        final String key = Math.random() + "";
        Glide.with(this).load(CODE_URL + "?key=" + key + "&scene=2&t=" + Math.random()).into(imageView);

        
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                edtInput.setText("");
                Glide.with(NDGAct.this).load(CODE_URL + "?key=" + key + "&scene=2&t=" + Math.random()).into(imageView);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle(R.string.inputvc);
        builder.setView(textEntryView);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edtInput.getWindowToken(), 0);
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        String serverUrl = UPLOAD_URL + "?code=" + edtInput.getText() + "&os=Android&module=" + Build.MODEL.replace(" ", "") + "&key=" + key;
                        showLoading("Uploading");
                        FileUtil.uploadFiles(NDGAct.this, new MyUploadDelegate(), serverUrl, "upload", Environment.getExternalStorageDirectory() + "/test.zip");
                    }
                };
                createZip(runnable);
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edtInput.getWindowToken(), 0);
            }
        });
        builder.show();
    }

    public void showFilter(final Context context, final int type) {
        BrowserMobProxy proxy = ((CrashApp) getApplication()).proxy;

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.alert_filter, null);
        ListView listView = (ListView) view.findViewById(R.id.list);
        List<HarPage> harPageList = proxy.getHar().getLog().getPages();
        final List<PageBean> pageBeenList = new ArrayList<>();

        for (HarPage harPage : harPageList) {
            PageBean pageBean = new PageBean();
            if (disablePages.contains(harPage.getId())) {
                pageBean.setSelected(false);
            }
            pageBean.setName(harPage.getTitle());
            pageBean.setCount(proxy.getHar(harPage.getId()).getLog().getEntries().size() + "");
            pageBeenList.add(pageBean);
        }

        listView.setAdapter(new PageFilterAdapter(pageBeenList));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle("Please select pagination");
        builder.setView(view);

        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton(R.string.cancel, null);

        class ConfirmListener implements View.OnClickListener {
            private AlertDialog alertDialog;

            public ConfirmListener(AlertDialog alertDialog) {
                this.alertDialog = alertDialog;
            }

            @Override
            public void onClick(View v) {
                disablePages.clear();

                int entryCount = 0;
                int selectedCount = 0;
                for (PageBean pageBean : pageBeenList) {
                    if (!pageBean.getSelected()) {
                        disablePages.add(pageBean.getName());
                    } else {
                        entryCount += pageBean.getCountInt();
                        selectedCount++;
                    }
                }

                if (selectedCount > 0) {
                    PreviewFragment.getInstance().notifyHarChange();

                    if (type == TYPE_SHARE) {
                        shareZip();
                    }
                    if (type == TYPE_UPLOAD) {
                        if (selectedCount > 1 && entryCount > 1000) {
                            Toast.makeText(context, R.string.toomanyreq,
                                    Toast.LENGTH_LONG).show();
                        }
                        uploadZip();
                    }
                    alertDialog.dismiss();
                } else {
                    Toast.makeText(context, "Please select at least one pagination", Toast.LENGTH_LONG).show();
                }
            }
        }

        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
                        new ConfirmListener(alertDialog));
            }
        });

        alertDialog.show();
    }

    public Set<String> getPageSet() {
        BrowserMobProxy proxy = ((CrashApp) getApplication()).proxy;

        Set<String> pageSet = new HashSet<>();
        for (HarPage harPage : proxy.getHar().getLog().getPages()) {
            if (!disablePages.contains(harPage.getId())) {
                pageSet.add(harPage.getId());
            }
        }

        return pageSet;
    }

    public Har getFiltedHar() {
        BrowserMobProxy proxy = ((CrashApp) getApplication()).proxy;
        return proxy.getHar(getPageSet());
    }

    public void initFloatingActionMenu() {
        binding.fab.setClosedOnTouchOutside(true);
        AnimatorSet set = new AnimatorSet();

        ObjectAnimator scaleOutX = ObjectAnimator.ofFloat(binding.fab.getMenuIconView(), "scaleX", 1.0f, 0.2f);
        ObjectAnimator scaleOutY = ObjectAnimator.ofFloat(binding.fab.getMenuIconView(), "scaleY", 1.0f, 0.2f);

        ObjectAnimator scaleInX = ObjectAnimator.ofFloat(binding.fab.getMenuIconView(), "scaleX", 0.2f, 1.0f);
        ObjectAnimator scaleInY = ObjectAnimator.ofFloat(binding.fab.getMenuIconView(), "scaleY", 0.2f, 1.0f);

        scaleOutX.setDuration(50);
        scaleOutY.setDuration(50);

        scaleInX.setDuration(150);
        scaleInY.setDuration(150);

        scaleInX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                binding.fab.getMenuIconView().setImageResource(binding.fab.isOpened()
                        ? R.drawable.ic_file_upload_white_24dp : R.drawable.ic_close_white_24dp);
                if(mBackHandedFragment instanceof PreviewFragment){
                    if(binding.fab.isOpened()){
                        binding.fabClear.show(true);
                    }else {
                        binding.fabClear.hide(true);
                    }
                }
            }
        });

        set.play(scaleOutX).with(scaleOutY);
        set.play(scaleInX).with(scaleInY).after(scaleOutX);
        set.setInterpolator(new OvershootInterpolator(2));

        binding.fab.setIconToggleAnimatorSet(set);

        binding.fabShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilter(NDGAct.this,TYPE_SHARE);
                binding.fab.close(true);
            }
        });

        binding.fabUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilter(NDGAct.this,TYPE_UPLOAD);
                binding.fab.close(true);
            }
        });

        binding.fabPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchContent(PreviewFragment.getInstance());
                binding.fab.close(true);
            }
        });

        binding.fabClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(NDGAct.this);
                builder.setTitle("Please confirm whether to clear all requests?");
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ((CrashApp)getApplication()).proxy.getHar().getLog().clearAllEntries();
                        PreviewFragment.getInstance().notifyHarChange();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
                builder.create().show();
            }
        });
    }

    private String[] uaItem = new String[]{"Phone Browser", "Wechat Environment", "QQ Environment"};

    public void showUaDialog() {
        DialogInterface.OnClickListener buttonListener = new ButtonOnClick();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String selected = SharedPreferenceUtils.getString(this, "select_ua", "0");
        int pos;
        try {
            pos = Integer.parseInt(selected);
        } catch (NumberFormatException e) {
            pos = -1;
        }
        builder.setTitle("Environment switch");
        builder.setSingleChoiceItems(uaItem, pos, buttonListener);
        builder.setPositiveButton(R.string.ok, buttonListener);
        builder.setNegativeButton(R.string.cancel, buttonListener);
        builder.create().show();
    }

    private class ButtonOnClick implements DialogInterface.OnClickListener {

        private int index = -1; 

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which >= 0) {
                index = which;
            } else {
                
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    SharedPreferenceUtils.putString(NDGAct.this, "select_ua", index + "");
                    WebViewFragment.getInstance().setUserAgent();
                }
            }
        }
    }

    public void showLogDialog(){
        View textEntryView = LayoutInflater.from(this).inflate(R.layout.alert_textview, null);
        TextView edtInput = (TextView) textEntryView.findViewById(R.id.tv_content);
        edtInput.setText(consoleLog);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Console.log");
        builder.setCancelable(true);
        builder.setView(textEntryView);
        builder.setPositiveButton(R.string.ok, null);
        builder.setNegativeButton("Empty the log", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                consoleLog.setLength(0);
            }
        });
        builder.show();
    }

    public void showHostDialog(){
        View textEntryView = LayoutInflater.from(this).inflate(R.layout.alert_edittext, null);
        final EditText editText = (EditText) textEntryView.findViewById(R.id.et_content);

        String host = SharedPreferenceUtils.getString(this, "system_host", "");
        editText.setText(host);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setView(textEntryView);
        builder.setTitle("HOST configuration");
        builder.setMessage("Configuring hosts, separated by spaces, one per line");
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                SharedPreferenceUtils.putString(NDGAct.this, "system_host", editText.getText()+"");
                DeviceUtils.changeHost(((CrashApp)getApplication()).proxy,editText.getText()+"");
            }
        });
        builder.setNegativeButton("Empty the line", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                SharedPreferenceUtils.putString(NDGAct.this, "system_host", "");
                DeviceUtils.changeHost(((CrashApp)getApplication()).proxy,editText.getText()+"");
            }
        });
        builder.show();
    }
}
