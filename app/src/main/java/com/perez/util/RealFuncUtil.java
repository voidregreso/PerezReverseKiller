package com.perez.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;

import androidx.core.content.res.ResourcesCompat;

import com.perez.revkiller.Features;
import com.perez.revkiller.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class RealFuncUtil {

    public static String getJPack(File fp) {
        try (BufferedReader rd = new BufferedReader(new FileReader(fp))) {
            String line;
            while ((line = rd.readLine()) != null) {
                if(line.contains("public class")) {
                    line = line.substring("public class".length());
                    int pos = line.indexOf("extends");
                    if(pos == -1) pos = line.indexOf("implements");
                    if(pos == -1) pos = line.indexOf(" ");
                    if(pos == -1) pos = line.indexOf("{");
                    return line.substring(0, pos).trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String md5(byte[] bytes) {
        if (bytes == null) return "null";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "null";
        }
    }

    public static byte[] getSvcSig(Context ctx) {
        try (ParcelFileDescriptor fd = ParcelFileDescriptor.adoptFd(Features.openFd(ctx.getPackageResourcePath()));
             ZipInputStream zis = new ZipInputStream(new FileInputStream(fd.getFileDescriptor()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().matches("(META-INF/.*)\\.(RSA|DSA|EC)")) {
                    CertificateFactory certFactory = CertificateFactory.getInstance("X509");
                    return ((X509Certificate) certFactory.generateCertificate(zis)).getEncoded();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getZipSig(Context ctx) {
        try (ZipFile zipFile = new ZipFile(ctx.getPackageResourcePath())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().matches("(META-INF/.*)\\.(RSA|DSA|EC)")) {
                    InputStream is = zipFile.getInputStream(entry);
                    CertificateFactory certFactory = CertificateFactory.getInstance("X509");
                    return ((X509Certificate) certFactory.generateCertificate(is)).getEncoded();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Drawable showApkIcon(Context ctx, String apkPath) {
        PackageManager pm = ctx.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        if(info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            try {
                return appInfo.loadIcon(pm);
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        }
        return ResourcesCompat.getDrawable(ctx.getResources(), R.drawable.android, null);
    }
}
