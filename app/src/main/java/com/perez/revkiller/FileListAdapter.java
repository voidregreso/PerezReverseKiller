package com.perez.revkiller;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.perez.util.FileUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FileListAdapter extends BaseAdapter {

    public static final int S_IFMT = 0170000;
    public static final int S_IFLNK = 0120000;
    public static final int S_IFREG = 0100000;
    public static final int S_IFBLK = 0060000;
    public static final int S_IFDIR = 0040000;
    public static final int S_IFCHR = 0020000;
    public static final int S_IFIFO = 0010000;

    protected final Context mContext;
    protected final LayoutInflater mInflater;
    private SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
    protected AsyncImageLoader asyn;

    public FileListAdapter(Context context) {
        mContext = context;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        asyn = new AsyncImageLoader(mContext);
    }

    public int getCount() {
        return getFileList().size();
    }

    public Object getItem(int position) {
        return getFileList().get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    private String permRwx(int perm) {
        String result;
        result = ((perm & 04) != 0 ? "r" : "-") + ((perm & 02) != 0 ? "w" : "-") + ((perm & 1) != 0 ? "x" : "-");
        return result;
    }

    private String permFileType(int perm) {
        String result = "?";
        switch(perm & S_IFMT) {
            case S_IFLNK:
                result = "l";
                break;
            case S_IFREG:
                result = "-";
                break;
            case S_IFBLK:
                result = "b";
                break;
            case S_IFDIR:
                result = "d";
                break;
            case S_IFCHR:
                result = "c";
                break;
            case S_IFIFO:
                result = "p";
                break;
        }
        return result;
    }

    public String permString(int perms) {
        String result;
        result = permFileType(perms) + permRwx(perms >> 6) + permRwx(perms >> 3) + permRwx(perms);
        return result;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        final File file = getFileList().get(position);
        String name = file.getName().toLowerCase();
        RelativeLayout container;
        if(convertView == null)
            container = (RelativeLayout) mInflater.inflate(R.layout.list_item_details, null);
        else
            container = (RelativeLayout) convertView;
        final ImageView icon = (ImageView) container.findViewById(R.id.icon);
        if(file.isDirectory())
            icon.setImageResource(R.drawable.folder);
        else if(name.endsWith(".apk")) {
            Drawable drawable = asyn.loadDrawable(file.getAbsolutePath(), icon, new PerezReverseKillerMain.ImageCallback() {
                public void imageLoaded(Drawable drawable, ImageView imageView) {
                    icon.setImageDrawable(drawable);
                }
            });
            icon.setImageDrawable(drawable);
        } else if(name.endsWith(".png") || name.endsWith(".jpg"))
            icon.setImageResource(R.drawable.image);
        else if(name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z"))
            icon.setImageResource(R.drawable.zip);
        else if(name.endsWith(".jar"))
            icon.setImageResource(R.drawable.jar);
        else if(name.endsWith(".so"))
            icon.setImageResource(R.drawable.sharedlib);
        else if(name.endsWith(".dex") || name.endsWith(".odex") || name.endsWith(".oat"))
            icon.setImageResource(R.drawable.dex);
        else if(name.endsWith(".rc") || name.endsWith(".sh"))
            icon.setImageResource(R.drawable.script);
        else if(name.endsWith(".xml"))
            icon.setImageResource(R.drawable.xml);
        else if(name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".c") || name.endsWith(".cpp")
                || name.endsWith(".cs") || name.endsWith(".h") || name.endsWith(".hpp") || name.endsWith(".java")
                || name.endsWith(".smali"))
            icon.setImageResource(R.drawable.text);
        else if(name.endsWith(".arsc"))
            icon.setImageResource(R.drawable.arsc);
        else if(name.endsWith(".pdf"))
            icon.setImageResource(R.drawable.pdf);
        else if(name.endsWith(".xls") || name.endsWith(".xlsx"))
            icon.setImageResource(R.drawable.excel);
        else if(name.endsWith(".ppt") || name.endsWith(".pptx") || name.endsWith(".pps")
                || name.endsWith(".ppsx"))
            icon.setImageResource(R.drawable.ppt);
        else if(name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".dot")
                || name.endsWith(".dotx"))
            icon.setImageResource(R.drawable.word);
        else if(name.endsWith(".mp4") || name.endsWith(".3gp") || name.endsWith(".avi") || name.endsWith(".wmv")
                || name.endsWith(".vob") || name.endsWith(".ts") || name.endsWith(".flv") || name.endsWith(".rm")
                || name.endsWith(".rmvb") || name.endsWith(".f4v") || name.endsWith(".mov")
                || name.endsWith(".webm") || name.endsWith(".mpg") || name.endsWith(".asf")
                || name.endsWith(".mkv"))
            icon.setImageResource(R.drawable.video);
        else if(name.endsWith(".mp3") || name.endsWith(".aac") || name.endsWith(".mp2") || name.endsWith(".wav")
                || name.endsWith(".wma") || name.endsWith(".ogg") || name.endsWith(".ape")
                || name.endsWith(".amr"))
            icon.setImageResource(R.drawable.audio);
        else
            icon.setImageResource(R.drawable.file);
        TextView text = (TextView) container.findViewById(R.id.text);
        TextView perm = (TextView) container.findViewById(R.id.permissions);
        TextView time = (TextView) container.findViewById(R.id.times);
        TextView size = (TextView) container.findViewById(R.id.size);
        text.setText(file.getName());
        String perms;
        try {
            perms = permString(FileUtil.getPermissions(file));
        } catch(Exception e) {
            perms = "????";
        }
        perm.setText(perms);
        Date date = new Date(file.lastModified());
        time.setText(format.format(date));
        if(file.isDirectory())
            size.setText("");
        else
            size.setText(MiscellaneousFunctions.convertBytesLength(file.length()));
        return container;
    }

    protected List<File> getFileList() {
        return PerezReverseKillerMain.mFileList;
    }
}