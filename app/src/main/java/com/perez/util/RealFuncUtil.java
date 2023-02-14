package com.perez.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.perez.revkiller.Features;
import com.perez.revkiller.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RealFuncUtil {

    public static String getJPack(File fp) {
        BufferedReader rd;
        try {
            rd = new BufferedReader(new FileReader(fp));
            String nuevo;
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


    private static String md5(byte[] bytes) {
        if (bytes == null) return "";
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(bytes);
            String hexDigits = "0123456789abcdef";
            char[] str = new char[digest.length * 2];
            int k = 0;
            for (byte b : digest) {
                str[k++] = hexDigits.charAt(b >>> 4 & 0xf);
                str[k++] = hexDigits.charAt(b & 0xf);
            }
            return new String(str);
        } catch (NoSuchAlgorithmException e) {
            return "null";
        }
    }

    public static String getSvcSig(Context ctx) {
        try (ParcelFileDescriptor fd = ParcelFileDescriptor.adoptFd(Features.openFd(ctx.getPackageResourcePath()));
            ZipInputStream zis = new ZipInputStream(new FileInputStream(fd.getFileDescriptor()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().matches("(META-INF/.*)\\.(RSA|DSA|EC)")) {
                    CertificateFactory certFactory = CertificateFactory.getInstance("X509");
                    X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(zis);
                    return md5(x509Cert.getEncoded());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "null";
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
