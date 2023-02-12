package com.perez;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.perez.revkiller.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class RealFuncUtil {

    public static String getJPack(File fp) {
        BufferedReader rd;
        try {
            rd = new BufferedReader(new FileReader(fp));
            String line0 = rd.readLine().substring("package ".length());
            line0 = line0.substring(0, line0.length()-1);
            String nuevo = "";
            do {
                nuevo = rd.readLine();
                if(nuevo.contains("public class")) {
                    nuevo = nuevo.substring("public class".length());
                    int pos = nuevo.indexOf("extends");
                    if(pos == -1) pos = nuevo.indexOf("implements");
                    if(pos == -1) pos = nuevo.indexOf(" ");
                    if(pos == -1) pos = nuevo.indexOf("{");
                    nuevo = nuevo.substring(0, pos);
                    return nuevo;
                }
            } while(nuevo != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static Drawable showApkIcon(Context ctx, String apkPath) {
        PackageManager pm = ctx.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkPath,
                PackageManager.GET_ACTIVITIES);
        if(info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            try {
                return appInfo.loadIcon(pm);
            } catch(OutOfMemoryError e) {
                e.printStackTrace();
            }
        }
        return ctx.getResources().getDrawable(R.drawable.android);
    }
}
