package com.perez.catchexception;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.multidex.MultiDex;

import com.github.karthyks.crashlytics.Crashlytics;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.proxy.CaptureType;
import net.lightbody.bmp.proxy.dns.AdvancedHostResolver;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.perez.netdiagnosis.Bean.ResponseFilterRule;
import com.perez.netdiagnosis.Utils.DeviceUtils;
import com.perez.netdiagnosis.Utils.SharedPreferenceUtils;
import com.perez.revkiller.R;

public class CrashApp extends Application {
    CrashApp instance;
    public static Boolean isInitProxy = false;
    public static int proxyPort = 8888;
    public BrowserMobProxy proxy;
    public List<ResponseFilterRule> ruleList = new ArrayList<>();

    public void initProxy() {
        try {
            FileUtils.forceMkdir(new File(Environment.getExternalStorageDirectory() + "/har"));
        } catch (IOException e) {
        }

        new Thread(() -> {
            startProxy();
            Intent intent = new Intent();
            intent.setAction("proxyfinished");
            sendBroadcast(intent);
        }).start();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        new Thread(() -> {
            proxy.stop();
        }).start();
    }

    private void attemptProxy(int port) {
        proxy = new BrowserMobProxyServer();
        proxy.setTrustAllServers(true);
        proxy.start(port);
    }

    public void startProxy() {
        try {
            attemptProxy(8888);
        } catch (Throwable t) {
            try {
                Random rand = new Random();
                proxyPort = rand.nextInt(1000) + 8000;
                attemptProxy(proxyPort);
            } catch (Throwable th) {
                th.printStackTrace();
                return;
            }
        }
        Log.d("~~~", "Bound port: " + proxy.getPort());

        Object object = SharedPreferenceUtils.get(this.getApplicationContext(), "response_filter");
        if (object instanceof List) {
            ruleList = (List<ResponseFilterRule>) object;
        }

        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (shp.getBoolean("enable_filter", false)) {
            initResponseFilter();
        }

        if (!shp.getString("system_host", "").isEmpty()) {
            AdvancedHostResolver advancedHostResolver = proxy.getHostNameResolver();
            for (String temp : shp.getString("system_host", "").split("\\n")) {
                if (temp.split(" ").length == 2) {
                    advancedHostResolver.remapHost(temp.split(" ")[1], temp.split(" ")[0]);
                }
            }
            proxy.setHostNameResolver(advancedHostResolver);
        }

        proxy.enableHarCaptureTypes(CaptureType.REQUEST_HEADERS, CaptureType.REQUEST_COOKIES,
                CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_HEADERS, CaptureType.REQUEST_COOKIES,
                CaptureType.RESPONSE_CONTENT);

        isInitProxy = true;
    }

    private void initResponseFilter() {
        try {
            if (ruleList == null) {
                ResponseFilterRule rule = new ResponseFilterRule();
                rule.setUrl("xw.qq.com/index.htm");
                rule.setReplaceRegex("</head>");
                rule.setReplaceContent("<script>alert('Test of package modification')</script></head>");
                ruleList = new ArrayList<>();
                ruleList.add(rule);
            }

            DeviceUtils.changeResponseFilter(this, ruleList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        instance = this;
    }

    public void onCreate() {
        super.onCreate();
        Crashlytics.init(this, events -> {
            Log.d("MainApplication", "onEventOccurred: " + events.size());
        });
        initProxy();
        try {
            System.loadLibrary("function");
        } catch (UnsatisfiedLinkError u) {
            u.printStackTrace();
            Toast.makeText(getApplicationContext(),
                    R.string.unsatisfied_link_err, Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(-1);
        }
    }
}
