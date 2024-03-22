package com.perez.netdiagnosis.Utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import android.widget.Toast;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.UploadStatusDelegate;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by xuzhou on 2016/9/12.
 * FileUtil
 */

public class FileUtil {

    public static String getMd5ByFile(File file) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            MessageDigest digester = MessageDigest.getInstance("MD5"); // TODO
            
            byte[] bytes = new byte[8192];
            int byteCount;
            while ((byteCount = in.read(bytes)) > 0) {
                digester.update(bytes, 0, byteCount);
            }
            byte[] digest = digester.digest();
            BigInteger bi = new BigInteger(1, digest);
            return String.format("%032x", bi);
        } catch (Exception e) {
            return "";
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getMd5(String source) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(source.getBytes());
            BigInteger bi = new BigInteger(1, md5.digest());
            return String.format("%032x", bi);
        } catch (Exception e) {
            return "";
        }
    }

    public static File getDataRoot() {
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||
                    (Environment.getExternalStorageDirectory().exists() && Environment.getExternalStorageDirectory().canWrite())) {
                return Environment.getExternalStorageDirectory();
            } else {
                return Environment.getDataDirectory();
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static File getAppRoot() {
        try {
            File root = new File(getDataRoot(), "com.darkal.nt");
            if (!root.exists()) {
                root.mkdirs();
            }

            
            File nomedia = new File(root, ".nomedia");
            if (!nomedia.exists()) {
                nomedia.createNewFile();
            }

            return root;
        } catch (Exception e) {
            return null;
        }
    }

    public static File getModuleRoot() {
        try {
            File root = new File(getAppRoot(), "module");
            if (!root.exists()) {
                root.mkdirs();
            }
            return root;
        } catch (Exception e) {
            return null;
        }
    }

    public static File getUpdateRoot() {
        try {
            File root = new File(getAppRoot(), "update");
            if (!root.exists()) {
                root.mkdirs();
            }
            return root;
        } catch (Exception e) {
            return null;
        }
    }

    public static File getConfigRoot() {
        File root = new File(getAppRoot(), "config");
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    public static File getLogRoot() {
        File root = new File(getAppRoot(), "logs");
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    public static File getDcimRoot() {
        File root = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "/JZYC/");
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    public static File getWardrobeRoot() {
        File root = new File(getAppRoot(), "wardrobe");
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    public static File getHeadImageRoot() {
        File root = new File(getAppRoot(), "headimage");
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    public static File getMatchRoot() {
        File root = new File(getAppRoot(), "match");
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    public static File getCopyRoot() {
        File root = new File(getAppRoot(), "copy");
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    private static File getFileByPath(String path, boolean isDir) {
        File fileModuleRoot = getModuleRoot();
        File file = new File(fileModuleRoot, path);
        if (!file.exists()) {
            if (isDir) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();
                file = new File(fileModuleRoot, path);
            }
        }
        return file;
    }

    public static void unzipFile(String filePath, String unzipPath) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(filePath);
            for (Enumeration entry = zipFile.entries(); entry.hasMoreElements(); ) {
                ZipEntry zipEntry = (ZipEntry) entry.nextElement();
                if (zipEntry.isDirectory()) {
                    
                    continue;
                }
                
                if (zipEntry.getSize() > 0) {
                    File file = FileUtil.getFileByPath(unzipPath + "/" + zipEntry.getName(), false);
                    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                    InputStream is = zipFile.getInputStream(zipEntry);
                    byte[] buffer = new byte[4096];
                    int len = 0;
                    while ((len = is.read(buffer)) >= 0) {
                        os.write(buffer, 0, len);
                    }

                    os.flush();
                    os.close();
                }
                
                else {
                    FileUtil.getFileByPath(unzipPath + "/" + zipEntry.getName(), true);
                }
            }
            zipFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void unzipAssetModule(Context context) {
        if ((context == null) || (context.getResources() == null) || (context.getResources().getAssets() == null)) {
            return;
        }
        ZipInputStream in = null;
        OutputStream os = null;
        try {
            InputStream is = context.getResources().getAssets().open("module.zip");
            in = new ZipInputStream(is);
            ZipEntry entry = in.getNextEntry();
            while (entry != null) {
                if (entry.isDirectory()) {
                    File file = new File(FileUtil.getAppRoot(), entry.getName());
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                } else {
                    File file = new File(FileUtil.getAppRoot(), entry.getName());
                    os = new BufferedOutputStream(new FileOutputStream(file));
                    byte[] buffer = new byte[1000000];
                    int len = 0;
                    while ((len = in.read(buffer)) >= 0) {
                        os.write(buffer, 0, len);
                    }
                    os.flush();
                }
                entry = in.getNextEntry();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void copyDir(String src, String dst) {
        try {
            File fileSrc = new File(src);
            if (!fileSrc.exists()) {
                return;
            }
            File[] filelist = fileSrc.listFiles();
            File fileDst = new File(dst);
            if (!fileDst.exists()) {
                fileDst.mkdirs();
            }
            for (File f : filelist) {
                if (f.isDirectory()) {
                    copyDir(f.getPath() + "/", dst + f.getName() + "/");
                } else {
                    copyFile(f.getPath(), dst + f.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(String src, String dst) {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte[] b = new byte[1024 * 5];
            int len = 0;
            while ((len = in.read(b)) > 0) {
                out.write(b, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void deleteAlbumFile(Context context, String filePath) {
        File photo = new File(filePath);
        if (!photo.exists()) {
            return;
        }
        photo.delete();
        context.getContentResolver().delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Images.Media.DATA + "=?",
                new String[]{
                        photo.getAbsolutePath()
                });
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + photo)));
    }

    public static void saveImageToGallery(Context context, String filePath) {
        StringBuilder path = new StringBuilder();
        String[] aa = filePath.split("\\/");
        for (int i = 0; i < aa.length - 1; i++) {
            path.append(aa[i]);
            path.append("/");
        }
        
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(), path.toString(), filePath, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + filePath)));
    }

    
    public static void deleteFiles(File file){
        try {
            if (file.exists()) {
                if (file.isDirectory()) {
                    File[] childFiles = file.listFiles();
                    for (File f : childFiles) {
                        deleteFiles(f);
                    }
                } else {
                    file.delete();
                }
            }
        }catch (Exception e){}
    }

    public static void checkPermission(Activity activity,Runnable runnable) {
        
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(activity, "Please activate relevant permissions, otherwise you will not be able to use this application normally!", Toast.LENGTH_SHORT).show();
            }
            
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            runnable.run();
        }
    }


    public static void uploadFiles(Context context, UploadStatusDelegate uploadStatusDelegate, String serverUrlString, String paramNameString, String filesToUploadString) {
        final String[] filesToUploadArray = filesToUploadString.split(",");

        for (String fileToUploadPath : filesToUploadArray) {
            try {
                MultipartUploadRequest req = new MultipartUploadRequest(context, serverUrlString)
                        .addFileToUpload(fileToUploadPath, paramNameString).setMethod("POST")
                        .setMaxRetries(3);

                req.setDelegate(uploadStatusDelegate).startUpload();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
