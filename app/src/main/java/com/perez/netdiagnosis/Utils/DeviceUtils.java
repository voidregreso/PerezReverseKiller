package com.perez.netdiagnosis.Utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.perez.catchexception.CrashApp;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.filters.ResponseFilter;
import net.lightbody.bmp.proxy.dns.AdvancedHostResolver;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpMessageInfo;

import java.util.List;

import com.perez.netdiagnosis.Bean.ResponseFilterRule;
import io.netty.handler.codec.http.HttpResponse;

public class DeviceUtils {
    
    public static String getVersion(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static int getVersionCode(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    //dip To  px
    public static int dip2px(Context context, int dp) {
        
        float density = context.getResources().getDisplayMetrics().density;
        //2*1.5+0.5  2*0.75 = 1.5+0.5
        return (int)(dp*density+0.5);
    }


    public static void changeHost(BrowserMobProxy browserMobProxy,String newValue){
        AdvancedHostResolver advancedHostResolver = browserMobProxy.getHostNameResolver();
        advancedHostResolver.clearHostRemappings();
        for (String temp : newValue.split("\\n")) {
            if (temp.split(" ").length == 2) {
                advancedHostResolver.remapHost(temp.split(" ")[1], temp.split(" ")[0]);
                Log.e("~~~~remapHost ", temp.split(" ")[1] + " " + temp.split(" ")[0]);
            }
        }


        browserMobProxy.setHostNameResolver(advancedHostResolver);
    }

    public static void changeResponseFilter(CrashApp CrashApp, final List<ResponseFilterRule> ruleList){
        if(ruleList == null){
            Log.e("~~~~","changeResponseFilter ruleList == null!");
            return;
        }

        CrashApp.proxy.addResponseFilter(new ResponseFilter() {
            @Override
            public void filterResponse(HttpResponse response, HttpMessageContents contents, HttpMessageInfo messageInfo) {
                for (ResponseFilterRule rule: ruleList) {
                    if(rule.getEnable()) {
                        if (contents.isText() && messageInfo.getUrl().contains(rule.getUrl())) {
                            String originContent = contents.getTextContents();
                            if (originContent != null) {
                                contents.setTextContents(contents.getTextContents().replaceAll(
                                        rule.getReplaceRegex(), rule.getReplaceContent()));
                            }
                        }
                    }
                }
            }
        });
    }
}
