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

import es.perez.netdiagnosis.Bean.ResponseFilterRule;
import es.perez.netdiagnosis.Utils.DeviceUtils;
import es.perez.netdiagnosis.Utils.SharedPreferenceUtils;

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
            Log.e("~~~","onTerminate");
            proxy.stop();
        }).start();
    }

    public void startProxy(){
        try {
            proxy = new BrowserMobProxyServer();
            proxy.setTrustAllServers(true);
            proxy.start(8888);
        } catch (Exception e) {
            
            Random rand = new Random();
            int randNum = rand.nextInt(1000) + 8000;
            proxyPort = randNum;

            proxy = new BrowserMobProxyServer();
            proxy.setTrustAllServers(true);
            proxy.start(randNum);
        }
        Log.e("~~~", proxy.getPort() + "");


        Object object = SharedPreferenceUtils.get(this.getApplicationContext(), "response_filter");
        if (object != null && object instanceof List) {
            ruleList = (List<ResponseFilterRule>) object;
        }

        SharedPreferences shp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if(shp.getBoolean("enable_filter", false)) {
            Log.e("~~~enable_filter", "");
            initResponseFilter();
        }

        if(shp.getString("system_host", "").length()>0){
            AdvancedHostResolver advancedHostResolver = proxy.getHostNameResolver();
            for (String temp : shp.getString("system_host", "").split("\\n")){
                if(temp.split(" ").length==2) {
                    advancedHostResolver.remapHost(temp.split(" ")[1],temp.split(" ")[0]);
                    Log.e("~~~~remapHost ",temp.split(" ")[1] +" " + temp.split(" ")[0]);
                }
            }
            proxy.setHostNameResolver(advancedHostResolver);
        }

        proxy.enableHarCaptureTypes(CaptureType.REQUEST_HEADERS, CaptureType.REQUEST_COOKIES,
                CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_HEADERS, CaptureType.REQUEST_COOKIES,
                CaptureType.RESPONSE_CONTENT);

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                .format(new Date(System.currentTimeMillis()));
        proxy.newHar(time);

        isInitProxy = true;
    }

    private void initResponseFilter(){
        try {
            if(ruleList == null){
                ResponseFilterRule rule = new ResponseFilterRule();
                rule.setUrl("xw.qq.com/index.htm");
                rule.setReplaceRegex("</head>");
                rule.setReplaceContent("<script>alert('Test of package modification')</script></head>");
                ruleList = new ArrayList<>();
                ruleList.add(rule);
            }

            DeviceUtils.changeResponseFilter(this,ruleList);
        }catch (Exception e){
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
            // Log to your Cloud DB for future analytics.
            Log.d("MainApplication", "onEventOccurred: " + events.size());
        });
        initProxy();
        try {
            System.loadLibrary("function");
        } catch(UnsatisfiedLinkError u) {
            u.printStackTrace();
            Toast.makeText(getApplicationContext(), "PerezManager cannot load its library so the Application will exit automatically", Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(3000);
            } catch(InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.exit(-1);
        }
    }
}
