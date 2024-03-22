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
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_details, null);
            viewHolder = new ViewHolder();
            viewHolder.container = (RelativeLayout) convertView;
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.text = (TextView) convertView.findViewById(R.id.text);
            viewHolder.perm = (TextView) convertView.findViewById(R.id.permissions);
            viewHolder.time = (TextView) convertView.findViewById(R.id.times);
            viewHolder.size = (TextView) convertView.findViewById(R.id.size);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        setIcon(file, name, viewHolder.icon);
        viewHolder.text.setText(file.getName());
        String perms;
        try {
            perms = permString(FileUtil.getPermissions(file));
        } catch (Exception e) {
            perms = "????";
        }
        viewHolder.perm.setText(perms);
        Date date = new Date(file.lastModified());
        viewHolder.time.setText(format.format(date));
        if (file.isDirectory())
            viewHolder.size.setText("");
        else
            viewHolder.size.setText(MiscellaneousFunctions.convertBytesLength(file.length()));

        return convertView;
    }

    private void setIcon(final File file, String name, final ImageView icon) {
        if (file.isDirectory()) {
            icon.setImageResource(R.drawable.folder);
        } else {
            switch (getExtension(name)) {
                case "apk":
                    Drawable drawable = asyn.loadDrawable(file.getAbsolutePath(), icon, new PerezReverseKillerMain.ImageCallback() {
                        public void imageLoaded(Drawable drawable, ImageView imageView) {
                            icon.setImageDrawable(drawable);
                        }
                    });
                    icon.setImageDrawable(drawable);
                    break;
                case "png":
                case "jpg":
                    icon.setImageResource(R.drawable.image);
                    break;
                case "zip":
                case "rar":
                case "7z":
                    icon.setImageResource(R.drawable.zip);
                    break;
                case "jar":
                    icon.setImageResource(R.drawable.jar);
                    break;
                case "so":
                    icon.setImageResource(R.drawable.sharedlib);
                    break;
                case "dex":
                case "odex":
                case "oat":
                    icon.setImageResource(R.drawable.dex);
                    break;
                case "rc":
                case "sh":
                    icon.setImageResource(R.drawable.script);
                    break;
                case "xml":
                    icon.setImageResource(R.drawable.xml);
                    break;
                case "txt":
                case "log":
                case "c":
                case "cpp":
                case "cs":
                case "h":
                case "hpp":
                case "java":
                case "smali":
                    icon.setImageResource(R.drawable.text);
                    break;
                case "arsc":
                    icon.setImageResource(R.drawable.arsc);
                    break;
                case "pdf":
                    icon.setImageResource(R.drawable.pdf);
                    break;
                case "xls":
                case "xlsx":
                    icon.setImageResource(R.drawable.excel);
                    break;
                case "ppt":
                case "pptx":
                case "pps":
                case "ppsx":
                    icon.setImageResource(R.drawable.ppt);
                    break;
                case "doc":
                case "docx":
                case "dot":
                case "dotx":
                    icon.setImageResource(R.drawable.word);
                    break;
                case "mp4":
                case "3gp":
                case "avi":
                case "wmv":
                case "vob":
                case "ts":
                case "flv":
                case "rm":
                case "rmvb":
                case "f4v":
                case "mov":
                case "webm":
                case "mpg":
                case "asf":
                case "mkv":
                    icon.setImageResource(R.drawable.video);
                    break;
                case "mp3":
                case "aac":
                case "mp2":
                case "wav":
                case "wma":
                case "ogg":
                case "ape":
                case "amr":
                    icon.setImageResource(R.drawable.audio);
                    break;
                default:
                    icon.setImageResource(R.drawable.file);
            }
        }
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            return fileName.substring(dotIndex + 1);
        } else {
            return "";
        }
    }

    protected List<File> getFileList() {
        return PerezReverseKillerMain.mFileList;
    }

    static class ViewHolder {
        RelativeLayout container;
        ImageView icon;
        TextView text;
        TextView perm;
        TextView time;
        TextView size;
    }
}