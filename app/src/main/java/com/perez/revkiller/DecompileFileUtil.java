package com.perez.revkiller;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.perez.revkiller.bean.FileType;
import com.perez.jadx.ShowCodeActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

public class DecompileFileUtil {

    /**
     * 
     *
     * @param file
     * @return
     */
    public static FileType getFileType(File file) {
        if(file.isDirectory())
            return FileType.directory;
        String fileName = file.getName().toLowerCase();
        if(fileName.endsWith(".mp3"))
            return FileType.music;
        if(fileName.endsWith(".mp4") || fileName.endsWith(".avi")
                || fileName.endsWith(".3gp") || fileName.endsWith(".mov")
                || fileName.endsWith(".rmvb") || fileName.endsWith(".mkv")
                || fileName.endsWith(".flv") || fileName.endsWith(".rm"))
            return FileType.video;
        if(fileName.endsWith(".txt") || fileName.endsWith(".log") || fileName.endsWith(".xml") || fileName.endsWith(".conf"))
            return FileType.txt;
        if(fileName.endsWith(".java") || fileName.endsWith(".smali") || fileName.endsWith(".sh") || fileName.endsWith(".cpp") || fileName.endsWith(".c"))
            return FileType.decode;
        if(fileName.endsWith(".zip") || fileName.endsWith(".rar"))
            return FileType.zip;
        if(fileName.endsWith(".png") || fileName.endsWith(".gif")
                || fileName.endsWith(".jpeg") || fileName.endsWith(".jpg"))
            return FileType.image;
        if(fileName.endsWith(".apk") || fileName.endsWith(".dex"))
            return FileType.apk;
        return FileType.other;
    }

    /**
     * 
     */
    public static Comparator comparator = new Comparator<File>() {
        @Override
        public int compare(File file1, File file2) {
            if(file1.isDirectory() && file2.isFile())
                return -1;
            else if(file1.isFile() && file2.isDirectory())
                return 1;
            else
                return file1.getName().compareTo(file2.getName());
        }
    };

    /**
     * 
     *
     * @param file
     * @return
     */
    public static int getFileChildCount(File file) {
        int count = 0;
        if(file.isDirectory()) {
            File[] files = file.listFiles();
            for(File f : files) {
                if(f.isHidden()) continue;
                count++;
            }
        }
        return count;
    }

    /**
     * 
     *
     * @param size
     * @return
     */
    public static String sizeToChange(long size) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.00");  
        double G = size * 1.0 / 1024 / 1024 / 1024;
        if(G >= 1)
            return df.format(G) + " GB";
        double M = size * 1.0 / 1024 / 1024;
        if(M >= 1)
            return df.format(M) + " MB";
        double K = size * 1.0 / 1024;
        if(K >= 1)
            return df.format(K) + " KB";
        return size + " B";
    }

    /**
     * apk
     *
     * @param context
     * @param fileName
     */
    public static void openAppIntent(Context context, String fileName) {
        Intent intent = new Intent(context, PackageActivity.class);
        intent.putExtra("fileName", fileName);
        context.startActivity(intent);
    }

    /**
     * 
     *
     * @param context
     * @param file
     */
    public static void openImageIntent(Context context, File file) {
        Uri path = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(path, "image/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    /**
     * 
     *
     * @param context
     * @param file
     */
    public static void openTextIntent(Context context, File file) {
        Uri path = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(path, "text/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }


    public static void openDecodeIntent(Context context, ArrayList<String> info) {
        Intent intent = new Intent(context, ShowCodeActivity.class);
        intent.putStringArrayListExtra("fileName", info);
        context.startActivity(intent);
    }

    /**
     * 
     *
     * @param context
     * @param file
     */
    public static void openMusicIntent(Context context, File file) {
        Uri path = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setDataAndType(path, "audio/*");
        context.startActivity(intent);
    }

    /**
     * 
     *
     * @param context
     * @param file
     */
    public static void openVideoIntent(Context context, File file) {
        Uri path = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setDataAndType(path, "video/*");
        context.startActivity(intent);
    }

    /**
     * 
     *
     * @param context
     * @param file
     */
    public static void openApplicationIntent(Context context, File file) {
        Uri path = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setDataAndType(path, "application/*");
        context.startActivity(intent);
    }

    /**
     * app
     *
     * @param context
     * @param file
     */
    public static void sendFile(Context context, File file) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.putExtra(Intent.EXTRA_STREAM,
                       Uri.fromFile(file));
        share.setType("*/*");
        context.startActivity(Intent.createChooser(share, "Send"));
    }

}
