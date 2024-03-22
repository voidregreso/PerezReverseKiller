package com.perez.revkiller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.googlecode.d2j.dex.Dex2jar;

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MiscellaneousFunctions {

    private static boolean reuseReg = true;
    private static boolean debugInfo = false;
    private static boolean printIR = false;
    private static boolean optmizeSynchronized = true;
    private static boolean skipExceptions = true;
    private static boolean noCode = false;
    private static final String TEMP_DIR = "system_config";
    private static final String TEMP_FILE_NAME = "system_file";
    private static final String TEMP_FILE_NAME_MIME_TYPE = "application/octet-stream";
    private static final String SP_NAME = "device_info";
    private static final String SP_KEY_DEVICE_ID = "device_id";

    public static void DecompileJAR(String zipfile) {
        String strpath = (new File(zipfile)).getParent() + "/decompile";
        File fn = new File(strpath);
        if (!fn.exists())
            fn.mkdir();
        ConsoleDecompiler.DoDecompile(zipfile, strpath);
    }

    public static boolean isStandardJAR(String zip) {
        boolean value = false;
        try {
            ZipFile zipFile = new ZipFile(zip);
            Enumeration<ZipEntry> enu = (Enumeration<ZipEntry>) zipFile.entries();
            while (enu.hasMoreElements()) {
                ZipEntry zipElement = (ZipEntry) enu.nextElement();
                zipFile.getInputStream(zipElement);
                String fileName = zipElement.getName();
                if (fileName.endsWith(".class")) {
                    value = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private static byte[] getBytes(String filePath) {
        byte[] buffer = null;
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = fis.read(b)) != -1)
                bos.write(b, 0, n);
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public static void installProcess(File apk, Activity act) {
        boolean haveInstallPermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            haveInstallPermission = act.getPackageManager().canRequestPackageInstalls();
            if (!haveInstallPermission) {
                Log.d("PerezReverseKiller", "Did not have installing permissions");
                AlertDialog.Builder builder = new AlertDialog.Builder(act);
                builder.setTitle(act.getString(R.string.tips));
                builder.setMessage(R.string.need_perm_tips);
                builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startInstallPermissionSettingActivity(act);
                    }
                });
                builder.show();
                return;
            }
        }

        MiscellaneousFunctions.installApk(act, apk);
    }

    public static void DexTrans(File in, File out) throws IOException {
        byte[] dexb = getBytes(in.getAbsolutePath());
        Dex2jar.from(dexb)
                .reUseReg(reuseReg)
                .topoLogicalSort()
                .skipDebug(!debugInfo)
                .optimizeSynchronized(optmizeSynchronized)
                .printIR(printIR)
                .noCode(noCode)
                .skipExceptions(skipExceptions)
                .to(out);
    }

    public static void DexTrans(String in, String out) throws IOException {
        DexTrans(new File(in), new File(out));
    }

    public static String readCodeFromFile(Context ctx, String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Toast.makeText(ctx, "Error accessing or reading file " + filePath, Toast.LENGTH_LONG).show();
            return "";
        }
    }

    public static boolean writeCodeToFile(Context ctx, String textOut, String filePath) {
        String origFilePath = filePath + ".orig";
        try {
            Files.move(Paths.get(filePath), Paths.get(origFilePath));
        } catch (IOException e) {
            Toast.makeText(ctx, "Cannot create backup file " + origFilePath, Toast.LENGTH_LONG).show();
            return false;
        }
        try {
            Files.write(Paths.get(filePath), textOut.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            Toast.makeText(ctx, "Cannot write to output " + filePath, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public static boolean formatting_code(Context ctx, String fn_path) {
        String textIn = readCodeFromFile(ctx, fn_path);
        String options = "-style=java";
        String textOut = Features.AStyleMain(textIn, options);
        if (textIn.isEmpty())
            return false;
        if (!writeCodeToFile(ctx, textOut, fn_path))
            return false;
        return true;
    }

    public static String convertSeconds(long tm) {
        long hour, minute, second;
        second = tm;
        hour = second / 3600;
        minute = (second - hour * 3600) / 60;
        second = second - hour * 300 - minute * 60;
        String str = String.format("%l:%.2l:%.2l", hour, minute, second);
        return str;
    }

    public static String convertBytesLength(long size) {
        DecimalFormat formater = new DecimalFormat("####.00");
        if (size < 1024)
            return size + "B";
        else if (size < 1024 * 1024) {
            float kbsize = size / 1024f;
            return formater.format(kbsize) + "KB";
        } else if (size < 1024 * 1024 * 1024) {
            float mbsize = size / 1024f / 1024f;
            return formater.format(mbsize) + "MB";
        } else {
            float gbsize = size / 1024f / 1024f / 1024f;
            return formater.format(gbsize) + "GB";
        }
    }

    public static boolean isZip(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".apk"))
            return true;
        if (name.endsWith(".zip") || name.endsWith(".jar"))
            return true;
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void startInstallPermissionSettingActivity(Activity ctx) {
        Uri packageURI = Uri.parse("package:" + ctx.getPackageName());

        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
        ctx.startActivityForResult(intent, 10086);
    }

    public static void installApk(Activity ctx, File apk) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
        } else {
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getApplicationContext().getPackageName() + ".provider", apk);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.getBaseContext().startActivity(intent);
    }

    public static String getDeviceId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String deviceId = sharedPreferences.getString(SP_KEY_DEVICE_ID, null);
        if (!TextUtils.isEmpty(deviceId)) {
            return deviceId;
        }
        deviceId = getIMEI(context);
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = createUUID(context);
        }
        sharedPreferences.edit()
                .putString(SP_KEY_DEVICE_ID, deviceId)
                .apply();
        return deviceId;
    }

    public static String createUUID(Context context) {
        String uuid = UUID.randomUUID().toString().replace("-", "");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            Uri externalContentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            ContentResolver contentResolver = context.getContentResolver();

            try (Cursor query = contentResolver.query(
                    externalContentUri,
                    new String[]{MediaStore.Downloads._ID},
                    MediaStore.Downloads.TITLE + "=?",
                    new String[]{TEMP_FILE_NAME},
                    null)) {
                if (query != null && query.moveToFirst()) {
                    Uri uri = ContentUris.withAppendedId(externalContentUri, query.getLong(0));
                    try (InputStream inputStream = contentResolver.openInputStream(uri);
                         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                        uuid = bufferedReader.readLine();
                    }
                } else {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.Downloads.TITLE, TEMP_FILE_NAME);
                    contentValues.put(MediaStore.Downloads.MIME_TYPE, TEMP_FILE_NAME_MIME_TYPE);
                    contentValues.put(MediaStore.Downloads.DISPLAY_NAME, TEMP_FILE_NAME);
                    contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + TEMP_DIR);

                    try (OutputStream outputStream = contentResolver.openOutputStream(contentResolver.insert(externalContentUri, contentValues))) {
                        if (outputStream != null) {
                            outputStream.write(uuid.getBytes());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            File externalDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File applicationFileDir = new File(externalDownloadsDir, TEMP_DIR);
            if (!applicationFileDir.exists() && !applicationFileDir.mkdirs()) {
                System.out.println("Unable to create directory: " + applicationFileDir.getPath());
            }

            File file = new File(applicationFileDir, TEMP_FILE_NAME);
            if (!file.exists()) {
                try (FileWriter fileWriter = new FileWriter(file, false)) {
                    fileWriter.write(uuid);
                } catch (IOException e) {
                    System.out.println("Unable to write to file: " + file.getPath());
                    e.printStackTrace();
                }
            } else {
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
                    uuid = bufferedReader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return uuid;
    }

    public static String getIMEI(Context context) {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) {
                return null;
            }
            @SuppressLint({ "MissingPermission", "HardwareIds" })
            String imei = telephonyManager.getDeviceId();
            return imei;
        } catch (Exception e) {
            return null;
        }
    }

}
