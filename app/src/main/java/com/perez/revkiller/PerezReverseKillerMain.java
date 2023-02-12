package com.perez.revkiller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.android.dx.J2DMain;
import com.perez.util.RealFuncUtil;
import com.perez.ap.AudioPlayer;
import com.perez.arsceditor.ArscActivity;
import com.perez.elfeditor.ElfActivity;
import com.perez.imageviewer.HugeImageViewerActivity;
import com.perez.palette.SketchActivity;
import com.perez.qrcode.CaptureActivity;
import com.perez.revkiller.exifremover.Interfaz;
import com.perez.util.FileUtil;
import com.perez.util.ZipExtract;
import com.perez.vp.VideoPlayerActivity;
import com.perez.xml2axml.func.FuncMain;

import org.glavo.javah.JavahTask;
import org.jb.dexlib.DexFile;
import org.jf.baksmali.BakSmaliFunc;
import org.jf.smali.Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;

import es.perez.netdiagnosis.Activity.NDGAct;

public class PerezReverseKillerMain extends AppCompatActivity {

    private final static String EMPTY = "";
    public final static String ENTRYPATH = "ZipEntry";
    public final static String SELECTEDMOD = "selected_mod";
    public final static String TAG = "PerezReverseKillerMain";

    public static final int SHOWPROGRESS = 1;
    public static final int DISMISSPROGRESS = 2;
    public static final int TOAST = 3;
    public static final int SHOWMESSAGE = 5;

    public static final int RQ_PERMISSION = 0xffee;

    public boolean initialized = false;

    private Stack<Integer> pos = new Stack<>();

    public static List<File> mFileList;
    private FileListAdapter mAdapter;
    private boolean mSelectMod = false;
    private String mQuery = EMPTY;
    private File mCurrentDir;
    private File mCurrent;
    public ListView lv;

    public int position;

    private static boolean mCut;
    private static File mClipboard;
    private Dialog mPermissionDialog;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case SHOWPROGRESS:
                PerezReverseKillerMain.this.showDialog(0);
                break;
            case DISMISSPROGRESS:
                mAdapter.notifyDataSetInvalidated();
                PerezReverseKillerMain.this.dismissDialog(0);
                break;
            case TOAST:
                toast(msg.obj.toString());
                break;
            case SHOWMESSAGE:
                showMessage(PerezReverseKillerMain.this, "", msg.obj.toString());
                break;
            }
        }
    };

    Comparator<File> sortByType = (file1, file2) -> {
        boolean a = file1.isDirectory();
        boolean b = file2.isDirectory();
        if(a && !b)
            return -1;
        else if(!a && b)
            return 1;
        else if(a && b)
            return file1.getName().toLowerCase().compareTo(file2.getName().toLowerCase());
        else
            return file1.getName().compareTo(file2.getName());
    };

    Comparator<File> sortByDate = (f1, f2) -> {
        boolean a = f1.isDirectory(), b = f2.isDirectory();
        if(a && !b) return -1;
        else if(!a && b) return 1;
        else {
            return new Long(f1.lastModified()).compareTo(new Long(f2.lastModified()));
        }
    };

    Comparator<File> sortBySize = (f1, f2) -> {
        boolean a = f1.isDirectory(), b = f2.isDirectory();
        if(a && !b) return -1;
        else if(!a && b) return 1;
        else {
            return new Long(f1.length()).compareTo(new Long(f2.length()));
        }
    };

    public void CreateInit() {
        lv = findViewById(R.id.zglist);
        handleIntent(getIntent());
        if(mCurrentDir == null) {
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                mCurrentDir = Environment.getExternalStorageDirectory();
            else
                mCurrentDir = Environment.getRootDirectory();
        }
        mAdapter = new FileListAdapter(getApplication());
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onInvalidated() {
                updateAndFilterFileList(EMPTY);
            }
        });
        registerForContextMenu(lv);
        updateAndFilterFileList(mQuery);
        lv.setAdapter(mAdapter);
        if(mPermissionDialog == null) {
            mPermissionDialog = new Dialog(this);
            mPermissionDialog.setContentView(R.layout.permissions);
            mPermissionDialog.findViewById(R.id.btnOk).setOnClickListener(v -> setPermissions());
            mPermissionDialog.findViewById(R.id.btnCancel).setOnClickListener(v -> mPermissionDialog.hide());
        }
        lv.setSelection(position);
        lv.setOnItemClickListener((parent, view, position, id) -> {
            final File file = (File) parent.getItemAtPosition(position);
            PerezReverseKillerMain.this.position = position;
            String name = file.getName();
            mCurrent = file;
            if(file.isDirectory()) {
                if(file.toString().endsWith("_baksmali")) {
                    isPreparedToBuildSmali = true;
                    new AlertDialog.Builder(PerezReverseKillerMain.this).setTitle(getString(R.string.tips)).
                            setMessage(getString(R.string.smali_instruction)).setPositiveButton(getString(R.string.build_smali), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            buildSmali(file);
                            isPreparedToBuildSmali = false;
                        }
                    }).setNeutralButton(getString(R.string.explore_dir), (arg0, arg1) -> {
                        mCurrentDir = file;
                        pos.push(parent.getFirstVisiblePosition());
                        mAdapter.notifyDataSetInvalidated();
                        isPreparedToBuildSmali = false;
                    }).show();
                } else {
                    mCurrentDir = file;
                    pos.push(parent.getFirstVisiblePosition());
                    mAdapter.notifyDataSetInvalidated();
                    return;
                }
            }
            if(mSelectMod) {
                mSelectMod = false;
                resultFileToZipEditor(file);
                return;
            }
            if(MiscellaneousFunctions.isZip(file))
                openApk(file);
            else if(name.toLowerCase().endsWith(".mp4") || name.toLowerCase().endsWith(".3gp")) {
                Intent intent = new Intent(PerezReverseKillerMain.this, VideoPlayerActivity.class);
                intent.setData(Uri.parse(file.toString()));
                startActivity(intent);
            } else if(name.toLowerCase().endsWith(".mp3") || name.toLowerCase().endsWith(".aac")
                      || name.toLowerCase().endsWith(".ogg") || name.toLowerCase().endsWith(".wma")
                      || name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".amr")) {
                Intent intent = new Intent(PerezReverseKillerMain.this, AudioPlayer.class);
                intent.putExtra("AUDIOPATH", file.toString());
                startActivity(intent);
            } else if(name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png")
                      || name.toLowerCase().endsWith(".bmp")) {
                Intent it = new Intent(PerezReverseKillerMain.this, HugeImageViewerActivity.class);
                it.putExtra("IMAGEPATH", file.toString());
                startActivity(it);
            } else if(name.toLowerCase().endsWith(".rar"))
                ExtractRar(file);
            else if(name.toLowerCase().endsWith(".odex"))
                ConOdex(file);
            else if(name.toLowerCase().endsWith(".oat"))
                OatToDex(file);
            else if(name.toLowerCase().endsWith(".so")) {
                if(!Features.isValidElf(file.toString())) {
                    Toast.makeText(PerezReverseKillerMain.this, getString(R.string.invalid_elf), Toast.LENGTH_LONG).show();
                    return;
                }
                PELF(file);
            } else if(name.toLowerCase().endsWith(".arsc"))
                editArsc(file);
            else if(name.toLowerCase().endsWith(".xml")) {
                if(FuncMain.isBinAXML(file.toString())) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(PerezReverseKillerMain.this);
                    builder.setTitle(getString(R.string.tips));
                    builder.setMessage(getString(R.string.axml_instruction));
                    builder.setPositiveButton("Decompile", (dialog, which) -> {
                        boolean sta = false;
                        mHandler.sendEmptyMessage(SHOWPROGRESS);
                        try {
                            FuncMain.decode(file.toString(),file.toString()+"_dec.xml");
                            sta = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mHandler.sendEmptyMessage(DISMISSPROGRESS);
                        if(sta) showToast("Failed to decompile AXML");
                        else showToast("Succeeded decompiling AXML");
                    });
                    builder.setNeutralButton("Edit AXML directly", (dialog, which) -> editAxml(file));
                    builder.show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(PerezReverseKillerMain.this);
                    builder.setTitle(getString(R.string.tips));
                    builder.setMessage(getString(R.string.xml_instruction));
                    builder.setPositiveButton("Compile", (dialog, which) -> {
                        boolean sta = false;
                        mHandler.sendEmptyMessage(SHOWPROGRESS);
                        try {
                            FuncMain.encode(PerezReverseKillerMain.this,file.toString(),file.toString()+"_comp.xml");
                            sta = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mHandler.sendEmptyMessage(DISMISSPROGRESS);
                        if(sta) showToast("Failed to compile XML");
                        else showToast("Succeeded compiling XML");
                    });
                    builder.setNeutralButton("Edit XML as plain text", (dialog, which) -> editText(file));
                    builder.show();
                }
            }

            else if(name.toLowerCase().endsWith(".txt") || name.toLowerCase().endsWith(".c")
                    || name.toLowerCase().endsWith(".cpp") || name.toLowerCase().endsWith(".java")
                    || name.toLowerCase().endsWith(".py") || name.toLowerCase().endsWith(".h")
                    || name.toLowerCase().endsWith(".hpp") || name.toLowerCase().endsWith(".cs")
                    || name.toLowerCase().endsWith(".smali"))
                editText(file);
            else if(name.toLowerCase().endsWith(".dex"))
                openDexFile(file);
            else {
                if(!isPreparedToBuildSmali)
                    dialogMenu();
            }
        });
        initialized = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
        case RQ_PERMISSION:
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                CreateInit();
            } else {
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(PerezReverseKillerMain.this);
                builder.setTitle("Alert");
                builder.setMessage("The application won't run unless you give the following necessary permissions.");
                builder.setPositiveButton("OK", (dialog, which) -> android.os.Process.killProcess(android.os.Process.myPid()));
                builder.show();
            }
            break;
        default:
            break;
        }
    }

    public boolean hasPermission(Context ctx, String[] perms) {
        for(String perm : perms) {
            if(ContextCompat.checkSelfPermission(ctx, perm) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listact);
        String[] pmList1 = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA};
        String[] pmList2 = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && (!hasPermission(this, pmList1) || !Environment.isExternalStorageManager())) {
            ActivityCompat.requestPermissions(PerezReverseKillerMain.this, pmList1, RQ_PERMISSION);
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + this.getPackageName()));
            startActivityForResult(intent, RQ_PERMISSION);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasPermission(this, pmList2)) {
            ActivityCompat.requestPermissions(PerezReverseKillerMain.this, pmList2, RQ_PERMISSION);
        } else {
            CreateInit();
        }
    }

    public static String getPkgSign(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            return new String(pi.signatures[0].toChars());
        } catch(Exception e) {
            e.printStackTrace();
            return "NULL";
        }
    }

    private void updateAndFilterFileList(final String query) {
        File[] files = mCurrentDir.listFiles();
        if(files != null) {
            setTitle(mCurrentDir.getPath());
            List<File> work = new Vector<File>(files.length);
            for(File file : files) {
                if(query == null || query.equals(EMPTY))
                    work.add(file);
                else if(file.getName().toLowerCase().contains(query.toLowerCase()))
                    work.add(file);
            }
            RankPrefUtil rpf = new RankPrefUtil(this);
            switch(rpf.GetWhich()) {
                case "type":
                default:
                    Collections.sort(work, sortByType);
                    break;
                case "date":
                    Collections.sort(work, sortByDate);
                    break;
                case "size":
                    Collections.sort(work, sortBySize);
                    break;
            }
            if(rpf.GetReverse()) Collections.reverse(work);
            mFileList = work;
            File parent = mCurrentDir.getParentFile();
            if(parent != null) {
                mFileList.add(0, new File(mCurrentDir.getParent()) {
                    @Override
                    public boolean isDirectory() {
                        return true;
                    }
                    @Override
                    public String getName() {
                        return "..";
                    }
                });
            }
        }
    }

    private void handleIntent(Intent intent) {
        mSelectMod = intent.getBooleanExtra(SELECTEDMOD, false);
    }

    private void resultFileToZipEditor(File file) {
        Intent intent = getIntent();
        intent.putExtra(ENTRYPATH, file.getAbsolutePath());
        setResult(ActResConstant.add_entry, intent);
        finish();
    }

    public void openApk(final File file) {
        if(file.toString().endsWith(".jar") && MiscellaneousFunctions.isStandardJAR(file.toString())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.tips));
            builder.setMessage(getString(R.string.jar_instruction));
            builder.setPositiveButton(getString(R.string.todex), (dialog, whichButton) -> new Thread(() -> {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                boolean JAR2DEX_SUC;
                try {
                    JAR2DEX_SUC = J2DMain.JarToDex(file.toString(),
                                                   file.toString().substring(0, file.toString().length() - 4) + "_converted.dex");
                } catch(IOException e) {
                    e.printStackTrace();
                    JAR2DEX_SUC = false;
                }
                if(!JAR2DEX_SUC)
                    showToast(getString(R.string.jar2dex_success));
                else
                    showToast(getString(R.string.jar2dex_fail));
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }).start());
            builder.setNegativeButton(getString(R.string.decompile_jar), (dialog, which) -> new Thread(() -> {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                MiscellaneousFunctions.DecompileJAR(file.toString());
                showToast(getString(R.string.djar_success));
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }).start());
            builder.setNeutralButton(getString(R.string.explore_jar), (dialog, whichButton) -> new Thread(() -> {
                Intent intent = new Intent(PerezReverseKillerMain.this, ZipManagerMain.class);
                ZipManagerMain.zipFileName = file.getAbsolutePath();
                startActivityForResult(intent, ActResConstant.list_item_details);
            }).start());
            builder.show();
        } else if(file.toString().endsWith(".apk")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.tips));
            builder.setMessage(getString(R.string.apk_instruction));
            builder.setPositiveButton(getString(R.string.open_apk), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Intent intent = new Intent(PerezReverseKillerMain.this, ZipManagerMain.class);
                    ZipManagerMain.zipFileName = file.getAbsolutePath();
                    startActivityForResult(intent, ActResConstant.list_item_details);
                }
            });
            builder.setNegativeButton(getString(R.string.decompile_javainapk), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DecompileFileUtil.openAppIntent(PerezReverseKillerMain.this, file.toString());
                }
            });
            builder.show();
        } else {
            Intent intent = new Intent(this, ZipManagerMain.class);
            ZipManagerMain.zipFileName = file.getAbsolutePath();
            startActivityForResult(intent, ActResConstant.list_item_details);
        }
    }

    private void editArsc(final File file) {
        new Thread(() -> {
            mHandler.sendEmptyMessage(SHOWPROGRESS);
            try {
                Intent it = new Intent(PerezReverseKillerMain.this, ArscActivity.class);
                it.putExtra("FilePath", file.toString());
                startActivityForResult(it, ActResConstant.list_item_details);
            } catch(Exception e) {
                Message msg = new Message();
                msg.what = SHOWMESSAGE;
                msg.obj = "Open Arsc exception " + e.getMessage();
                mHandler.sendMessage(msg);
            }
            mHandler.sendEmptyMessage(DISMISSPROGRESS);
        }).start();
    }

    private void editText(final File file) {
        new Thread(() -> {
            mHandler.sendEmptyMessage(SHOWPROGRESS);
            try {
                TextEditor.data = FileUtil.readFile(file);
                Intent intent = new Intent(PerezReverseKillerMain.this, TextEditor.class);
                intent.putExtra(TextEditor.PLUGIN, "TextEditor");
                startActivityForResult(intent, ActResConstant.list_item_details);
            } catch(Exception e) {
                Message msg = new Message();
                msg.what = SHOWMESSAGE;
                msg.obj = "Open Text exception " + e.getMessage();
                mHandler.sendMessage(msg);
            }
            mHandler.sendEmptyMessage(DISMISSPROGRESS);
        }).start();
    }

    private void editAxml(final File file) {
        new Thread(() -> {
            mHandler.sendEmptyMessage(SHOWPROGRESS);
            try {
                TextEditor.data = FileUtil.readFile(file);
                Intent intent = new Intent(PerezReverseKillerMain.this, TextEditor.class);
                intent.putExtra(TextEditor.PLUGIN, "AXmlEditor");
                startActivityForResult(intent, ActResConstant.list_item_details);
            } catch(Exception e) {
                Message msg = new Message();
                msg.what = SHOWMESSAGE;
                msg.obj = "Open Axml exception " + e.getMessage();
                mHandler.sendMessage(msg);
            }
            mHandler.sendEmptyMessage(DISMISSPROGRESS);
        }).start();
    }

    private void openDexFile(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.tips));
        builder.setMessage(getString(R.string.dex_instruction));
        builder.setPositiveButton(getString(R.string.tojar), (dialog, whichButton) -> new Thread(new Runnable() {
            public void run() {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                boolean DEX2JAR_SUC = true;
                try {
                    String dest_file = file.toString() + "_dex2jar.jar";
                    Log.d("PerezReverseKiller", "Dest file is : " + dest_file);
                    MiscellaneousFunctions.DexTrans(file.toString(), dest_file);
                } catch(Exception e) {
                    e.printStackTrace();
                    DEX2JAR_SUC = false;
                }
                if(!DEX2JAR_SUC)
                    showToast(getString(R.string.dex2jar_success));
                else
                    showToast(getString(R.string.dex2jar_fail));
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }
        }).start());
        builder.setNeutralButton(getString(R.string.editdex), (dialog, whichButton) -> new Thread(new Runnable() {
            public void run() {
                try {
                    mHandler.sendEmptyMessage(SHOWPROGRESS);
                    ClassListActivity.dexFile = new DexFile(file);
                    Intent intent = new Intent(PerezReverseKillerMain.this, ClassListActivity.class);
                    startActivityForResult(intent, ActResConstant.list_item_details);
                } catch(Exception e) {
                    Message msg = new Message();
                    msg.what = SHOWMESSAGE;
                    msg.obj = "Open dexFile exception " + e.getMessage();
                    mHandler.sendMessage(msg);
                }
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }
        }).start());
        builder.setNegativeButton(getString(R.string.disasm_dex), (dialog, which) -> new Thread(() -> {
            mHandler.sendEmptyMessage(SHOWPROGRESS);
            boolean DISDEX_SUC = BakSmaliFunc.DoBaksmali(file.toString(),
                                 file.toString().substring(0, file.toString().length() - 4) + "_baksmali");
            if(!DISDEX_SUC)
                showToast(getString(R.string.disdex_success));
            else
                showToast(getString(R.string.disdex_fail));
            mHandler.sendEmptyMessage(DISMISSPROGRESS);
        }).start());
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case ActResConstant.list_item_details:
            switch(resultCode) {
            case ActResConstant.text_editor:
                renameAndWrite();
                break;
            case ActResConstant.zip_list_item:
                mAdapter.notifyDataSetInvalidated();
                toast(ZipManagerMain.zipFileName);
                break;
            }
            break;
        }
    }

    private void renameAndWrite() {
        new Thread(() -> {
            mHandler.sendEmptyMessage(SHOWPROGRESS);
            FileOutputStream out = null;
            try {
                FileUtil.rename(mCurrent, mCurrent.getName() + ".bak");
                out = new FileOutputStream(mCurrent.getAbsolutePath());
                out.write(TextEditor.data);
            } catch(IOException io) {
            } finally {
                try {
                    if(out != null)
                        out.close();
                } catch(IOException e) {
                }
                TextEditor.data = null;
                System.gc();
            }
            Message msg = new Message();
            msg.what = TOAST;
            msg.obj = mCurrent.getName() + getString(R.string.saved);
            mHandler.sendMessage(msg);
            mHandler.sendEmptyMessage(DISMISSPROGRESS);
        }).start();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if(mSelectMod)
            return;
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.options);
        File file = null;
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            file = (File) lv.getItemAtPosition(info.position);
            if(!file.isDirectory())
                menu.add(Menu.NONE, R.string.view, Menu.NONE, R.string.view);
        } catch(ClassCastException e) {
            Log.e(TAG, "Bad menuInfo" + e);
        }
        String extn = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        menu.add(Menu.NONE, R.string.delete, Menu.NONE, R.string.delete);
        menu.add(Menu.NONE, R.string.rename, Menu.NONE, R.string.rename);
        if(MiscellaneousFunctions.isZip(file)) {
            menu.add(Menu.NONE, R.string.signed, Menu.NONE, R.string.signed);
            menu.add(Menu.NONE, R.string.extract_all, Menu.NONE, R.string.extract_all);
            menu.add(Menu.NONE, R.string.zipalign, Menu.NONE, R.string.zipalign);
        }
        menu.add(Menu.NONE, R.string.copy, Menu.NONE, R.string.copy);
        menu.add(Menu.NONE, R.string.cut, Menu.NONE, R.string.cut);
        menu.add(Menu.NONE, R.string.paste, Menu.NONE, R.string.paste);
        menu.add(Menu.NONE, R.string.permission, Menu.NONE, R.string.permission);
        if(extn.equals("c") || extn.equals("cpp") || extn.equals("java") || extn.equals("h") || extn.equals("cs")
                || extn.equals("hpp"))
            menu.add(Menu.NONE, R.string.fmtcode, Menu.NONE, R.string.fmtcode);
        if(extn.equals("jpg") || extn.equals("jpeg")) {
            menu.add(Menu.NONE, R.string.delexif, Menu.NONE, R.string.delexif);
            menu.add(Menu.NONE, R.string.str_jpg2png, Menu.NONE, R.string.str_jpg2png);
        }
        if(extn.equals("png")) {
            menu.add(Menu.NONE, R.string.str_png2jpg, Menu.NONE, R.string.str_png2jpg);
        }
        if(extn.equals("class")) {
            menu.add(Menu.NONE, R.string.genjni, Menu.NONE, R.string.genjni);
        }
    }

    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch(ClassCastException e) {
            Log.e(TAG, "Bad menuInfo" + e);
            return false;
        }
        mCurrent = (File) lv.getItemAtPosition(info.position);
        position = info.position;
        switch(item.getItemId()) {
        case R.string.delete:
            delete(mCurrent);
            return true;
        case R.string.view:
            viewCurrent();
            return true;
        case R.string.extract_all:
            extractAll(mCurrent);
            return true;
        case R.string.zipalign:
            zipAlign(mCurrent);
            return true;
        case R.string.signed:
            signedFile(mCurrent);
            return true;
        case R.string.rename:
            rename(mCurrent);
            return true;
        case R.string.copy:
            addCopy(mCurrent);
            return true;
        case R.string.cut:
            addCut(mCurrent);
            return true;
        case R.string.paste:
            pasteFile();
            return true;
        case R.string.permission:
            showPermissions();
            return true;
        case R.string.fmtcode:
            if(!MiscellaneousFunctions.formatting_code(this,mCurrent.getPath())) Toast.makeText(this, "Failed to format code", Toast.LENGTH_LONG).show();
            else Toast.makeText(this, "Succeeded formatting code", Toast.LENGTH_LONG).show();
            return true;
        case R.string.delexif:
            try {
                Interfaz.deleteEXIF(mCurrent.getPath(), mCurrent.getPath());
                Toast.makeText(this, "Succeeded deleting EXIF", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Cannot delete EXIF: " + e.toString() + " : " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            mAdapter.notifyDataSetInvalidated();
            return true;

        case R.string.str_jpg2png:
            FileUtil.convertToPng(mCurrent.getPath(), getFileNameNoEx(mCurrent.getPath())+".png");
            mAdapter.notifyDataSetInvalidated();
            break;

        case R.string.str_png2jpg:
            FileUtil.convertToJpg(mCurrent.getPath(), getFileNameNoEx(mCurrent.getPath())+".jpg");
            mAdapter.notifyDataSetInvalidated();
            break;

        case R.string.genjni:
            JavahTask task=new JavahTask();
            task.setOutputDir(Paths.get(mCurrentDir.toString()));
            task.addRuntimeSearchPath();
            String j2pack = RealFuncUtil.getJPack(mCurrent);
            task.addClass(j2pack);
            task.run();
            return true;
        }
        return false;
    }

    public void zipAlign(final File file) {
        new Thread(() -> {
            mHandler.sendEmptyMessage(SHOWPROGRESS);
            if(Features.isZipAligned(file.toString())) {
                showToast(getString(R.string.zip_has_aligned));
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
                return;
            }
            boolean b = Features.ZipAlign(file.toString(),
                                          file.toString().substring(0, file.toString().length() - 4) + "_aligned"
                                          + file.toString().substring(file.toString().length() - 4));
            if(b)
                showToast(getString(R.string.zipa_success));
            else
                showToast(getString(R.string.zipa_fail));
            mHandler.sendEmptyMessage(DISMISSPROGRESS);
        }).start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(mFileList != null && mFileList.size() > 0) {
                File first = mFileList.get(0);
                if(first.getName().equals("..") && first.getParentFile() != null) {
                    mCurrentDir = first;
                    mAdapter.notifyDataSetInvalidated();
                    if(!pos.empty())
                        lv.setSelection(pos.pop());
                    return true;
                }
            }
            if(mCurrentDir != null && mCurrentDir.getParentFile() != null) {
                mCurrentDir = mCurrentDir.getParentFile();
                mAdapter.notifyDataSetInvalidated();
                if(!pos.empty())
                    lv.setSelection(pos.pop());
                return true;
            }
            if(mCurrentDir != null && mCurrentDir.getParent() == null) {
                finish();
                if(!mSelectMod)
                    System.exit(0);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(initialized) mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        menu.add(Menu.NONE, R.string.add_folder, Menu.NONE, R.string.add_folder);
        menu.add(Menu.NONE, R.string.howto_rank, Menu.NONE, R.string.howto_rank);
        if(mClipboard != null)
            menu.add(Menu.NONE, R.string.paste, Menu.NONE, R.string.paste);
        menu.add(Menu.NONE, R.string.refresh, Menu.NONE, R.string.refresh);
        menu.add(Menu.NONE, R.string.dumpdex, Menu.NONE, R.string.dumpdex);
        menu.add(Menu.NONE, R.string.scan_qrcode, Menu.NONE, R.string.scan_qrcode);
        menu.add(Menu.NONE, R.string.httpcaptool, Menu.NONE, R.string.httpcaptool);
        menu.add(Menu.NONE, R.string.palette_app, Menu.NONE, R.string.palette_app);
        menu.add(Menu.NONE, R.string.about, Menu.NONE, R.string.about);
        if(!mSelectMod)
            menu.add(Menu.NONE, R.string.exit, Menu.NONE, R.string.exit);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void clearAll() {
        mCurrent = null;
        mClipboard = null;
        mCurrentDir = null;
        mCut = false;
        pos = null;
        System.gc();
    }

    public void Elegir() {
        RankPrefUtil rpf = new RankPrefUtil(this);
        final String[] criteria = {"By Type", "By Date", "By Size"};
        AtomicInteger selectedItem = new AtomicInteger();
        AtomicBoolean checked = new AtomicBoolean();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rank by");
        int cual;
        switch (rpf.GetWhich()) {
            case "type":
                cual = 0;
                break;
            case "date":
            default:
                cual = 1;
                break;
            case "size":
                cual = 2;
                break;
        }
        builder.setSingleChoiceItems(criteria, cual, (dialog, which) -> {
            selectedItem.set(which);
        });
        builder.setPositiveButton("OK", (dialog, which) -> {
            switch (selectedItem.get()) {
                case 0:
                    rpf.SetByType();
                    break;
                case 1:
                    rpf.SetByDate();
                    break;
                case 2:
                    rpf.SetBySize();
                    break;
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.setNeutralButton("Options", (dialog, which) -> {
            checked.set(rpf.GetReverse());
            final CheckBox checkBox = new CheckBox(this);
            checkBox.setText("Reversed ranking");
            checkBox.setChecked(checked.get());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                checked.set(isChecked);
            });
            AlertDialog checkBoxDialog = new AlertDialog.Builder(this).setTitle("Options")
                    .setView(checkBox)
                    .setPositiveButton("OK", (dialog1, which1) -> {
                        checked.set(checkBox.isChecked());
                        rpf.SetReverse(true);
                        dialog1.dismiss();
                    }).create();
            checkBoxDialog.show();
        });
        builder.create().show();
        mAdapter.notifyDataSetInvalidated();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch(itemId) {
        case R.string.add_folder:
            newFolder();
            break;
        case R.string.howto_rank:
            Elegir();
            break;
        case R.string.paste:
            pasteFile();
            break;
        case R.string.about:
            showAbout();
            break;
        case R.string.exit:
            finish();
            clearAll();
            System.exit(0);
            break;
        case R.string.palette_app:
            Intent i1 = new Intent(PerezReverseKillerMain.this, SketchActivity.class);
            startActivity(i1);
            break;
        case R.string.dumpdex: {
            LayoutInflater factory = LayoutInflater.from(this);
            final View view = factory.inflate(R.layout.editbox_layout, null);
            final EditText edit = (EditText)view.findViewById(R.id.editText1);
            edit.setHint("Please input the Application entrypoint you want to unpack,e.g. : com/example/hello/MainApplication");
            AlertDialog alg = new AlertDialog.Builder(PerezReverseKillerMain.this)
            .setTitle("Dex reinforce unpack and dump(BETA)")
            .setView(view)
            .setPositiveButton("OK", (dialog, which) -> {
                        String clz = edit.getText().toString();
                        if(clz.trim().isEmpty()) {
                            Toast.makeText(PerezReverseKillerMain.this, "Entrypoint cannot be null!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Features.dumpDex(21, clz);
                    }).setNegativeButton("Cancel", null).create();
            alg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            alg.show();
            break;
        }
        case R.string.refresh: {
            mAdapter.notifyDataSetInvalidated();
            break;
        }
        case R.string.scan_qrcode: {
            Intent i = new Intent(PerezReverseKillerMain.this, CaptureActivity.class);
            startActivity(i);
            break;
        }
        case R.string.httpcaptool: {
            Intent i0 = new Intent(PerezReverseKillerMain.this, NDGAct.class);
            startActivity(i0);
            break;
        }
        }
        return true;
    }

    private void signedFile(final File file) {
        new Thread(() -> {
            mHandler.sendEmptyMessage(SHOWPROGRESS);
            try {
                String out = file.getAbsolutePath();
                int i = out.lastIndexOf(".");
                if(i != -1)
                    out = out.substring(0, i) + ".firmado" + out.substring(i); // lo que está firmado
                apksigner.Main.sign(file, out);
                Message msg = new Message();
                msg.what = TOAST;
                msg.obj = out + getString(R.string.signed_success);
                mHandler.sendMessage(msg);
            } catch(Exception e) {
                Message msg = new Message();
                msg.what = SHOWMESSAGE;
                msg.obj = "signed error: " + e.getMessage();
                mHandler.sendMessage(msg);
            }
            mHandler.sendEmptyMessage(DISMISSPROGRESS);
        }).start();
    }

    private void extractAll(final File file) {
        String absName = file.getAbsolutePath();
        int i = absName.indexOf('.');
        if(i != -1)
            absName = absName.substring(0, i);
        absName += "_unpack";
        final EditText srcName = new EditText(this);
        srcName.setText(absName);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.extract_path);
        alert.setView(srcName);
        alert.setPositiveButton(R.string.btn_ok, (dialog, whichButton) -> {
            String src = srcName.getText().toString();
            if(src.length() == 0) {
                toast(getString(R.string.extract_path_empty));
                return;
            }
            new Thread(() -> {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                try {
                    ZipExtract.unzipAll(new ZipFile(file), new File(srcName.getText().toString()));
                } catch(Exception e) {
                }
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }).start();
        });
        alert.setNegativeButton(R.string.btn_cancel, null);
        alert.show();
    }

    private void dialogMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mCurrent.getName());
        builder.setItems(R.array.dialog_menu, (dialog, which) -> {
            switch(which) {
            case 0:
                viewCurrent();
                break;
            case 1:
                editText(mCurrent);
                break;
            case 2:
                delete(mCurrent);
                break;
            case 3:
                rename(mCurrent);
                break;
            case 4:
                addCopy(mCurrent);
                break;
            case 5:
                addCut(mCurrent);
                break;
            case 6:
                showPermissions();
                break;
            }
        });
        builder.show();
    }

    private void setPermBit(int perms, int bit, int id) {
        CheckBox ck = (CheckBox) mPermissionDialog.findViewById(id);
        ck.setChecked(((perms >> bit) & 1) == 1);
    }

    private int getPermBit(int bit, int id) {
        CheckBox ck = (CheckBox) mPermissionDialog.findViewById(id);
        int ret = (ck.isChecked()) ? (1 << bit) : 0;
        return ret;
    }

    public void showPermissions() {
        mPermissionDialog.setTitle(mCurrent.getName());
        try {
            int perms = FileUtil.getPermissions(mCurrent);
            setPermBit(perms, 8, R.id.ckOwnRead);
            setPermBit(perms, 7, R.id.ckOwnWrite);
            setPermBit(perms, 6, R.id.ckOwnExec);
            setPermBit(perms, 5, R.id.ckGrpRead);
            setPermBit(perms, 4, R.id.ckGrpWrite);
            setPermBit(perms, 3, R.id.ckGrpExec);
            setPermBit(perms, 2, R.id.ckOthRead);
            setPermBit(perms, 1, R.id.ckOthWrite);
            setPermBit(perms, 0, R.id.ckOthExec);
            mPermissionDialog.show();
        } catch(Exception e) {
            showMessage(this, "Permission Exception", e.getMessage());
        }
    }

    public void setPermissions() {
        mPermissionDialog.hide();
        int perms = getPermBit(8, R.id.ckOwnRead) | getPermBit(7, R.id.ckOwnWrite) | getPermBit(6, R.id.ckOwnExec)
                    | getPermBit(5, R.id.ckGrpRead) | getPermBit(4, R.id.ckGrpWrite) | getPermBit(3, R.id.ckGrpExec)
                    | getPermBit(2, R.id.ckOthRead) | getPermBit(1, R.id.ckOthWrite) | getPermBit(0, R.id.ckOthExec);
        try {
            FileUtil.chmod(mCurrent, perms);
            toast(Integer.toString(perms, 8));
            mAdapter.notifyDataSetChanged();
        } catch(Exception e) {
            showMessage(this, "Set Permission Exception", e.getMessage());
        }
    }

    private void viewCurrent() {
        String fn = mCurrent.toString();
        if(fn.substring(fn.lastIndexOf("."),fn.length()).equals(".apk")) {
            MiscellaneousFunctions.installProcess(mCurrent, this);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", mCurrent);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String mime = URLConnection.guessContentTypeFromName(uri.toString());
        if(mime != null) {
            if("text/x-java".equals(mime) || "text/xml".equals(mime))
                intent.setDataAndType(uri, "text/plain");
            else
                intent.setDataAndType(uri, mime);
        } else intent.setDataAndType(uri, "*/*");
        startActivity(intent);
    }

    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void addCopy(File file) {
        mClipboard = file;
        toast(getString(R.string.copy_to) + file.getName());
        mCut = false;
    }

    private void addCut(File file) {
        mClipboard = file;
        toast(getString(R.string.cut_to) + file.getName());
        mCut = true;
    }

    private void pasteFile() {
        String message = "";
        if(mClipboard == null) {
            showMessage(this, getString(R.string.copy_exception), getString(R.string.copy_nothing));
            return;
        }
        final File destination = new File(mCurrentDir, mClipboard.getName());
        if(destination.exists())
            message = String.format(getString(R.string.copy_message), destination.getName());
        if(message != "") {
            prompt(this, getString(R.string.over_write), message, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if(which == AlertDialog.BUTTON_POSITIVE)
                        performPasteFile(mClipboard, destination);
                }
            });
        } else
            performPasteFile(mClipboard, destination);
    }

    protected void performPasteFile(final File source, final File destination) {
        if(source.isDirectory())
            showMessage(this, getString(R.string.copy_exception), getString(R.string.copy_exist));
        else {
            new Thread(() -> {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                try {
                    FileUtil.copyFile(source, destination);
                    if(mCut)
                        source.delete();
                } catch(Exception e) {
                }
                mClipboard = null;
                Message msg = new Message();
                msg.what = TOAST;
                msg.obj = destination.getName() + getString(R.string.copied);
                mHandler.sendMessage(msg);
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }).start();
        }
    }

    public void ExtractRar(final File name) {
        new Thread(() -> {
            mHandler.sendEmptyMessage(SHOWPROGRESS);
            int results = Features.ExtractAllRAR(name.toString(),
                                                 name.toString().substring(0, name.toString().length() - 4) + "_extracted");
            if(results == 0)
                showToast(getString(R.string.extract_rar_success));
            else if(results == -1601)
                showToast(getString(R.string.rar_native_error));
            else
                showToast(getString(R.string.failed_to_extract_rar));
            mHandler.sendEmptyMessage(DISMISSPROGRESS);
        }).start();
    }

    public void OatToDex(final File name) {
        new Thread(() -> {
            mHandler.sendEmptyMessage(SHOWPROGRESS);
            String str = name.toString();
            boolean success1 = Features.Oat2Dex(str);
            mHandler.sendEmptyMessage(DISMISSPROGRESS);
            if(success1)
                showToast(getString(R.string.oat2dex_success));
            else
                showToast(getString(R.string.oat2dex_fail));
        }).start();
    }

    public void PELF(File name) {
        if(Features.isValidElf(name.toString())) {
            Intent i = new Intent(this, ElfActivity.class);
            i.putExtra("FILE_NAME", name.toString());
            startActivity(i);
            this.mAdapter.notifyDataSetInvalidated();
        }
    }

    public void ConOdex(final File name) {
        if(Features.isValidElf(name.toString()))
            OatToDex(name);
        else {
            new Thread(() -> {
                mHandler.sendEmptyMessage(SHOWPROGRESS);
                boolean success2 = Features.Odex2Dex(name.toString(),
                                                     name.toString().substring(0, name.toString().length() - 5) + "_converted.dex");
                if(success2)
                    showToast(getString(R.string.odex2dex_success));
                else
                    showToast(getString(R.string.odex2dex_fail));
                mHandler.sendEmptyMessage(DISMISSPROGRESS);
            }).start();
        }
    }

    public void buildSmali(final File name) {
        new Thread(() -> {
            mHandler.sendEmptyMessage(SHOWPROGRESS);
            String strproc = String.format("%d", Runtime.getRuntime().availableProcessors());
            String args[] = {"a", name.toString(), "-o", name.toString() + "_smali.dex", "-j", strproc};
            Main.main(args);
            mHandler.sendEmptyMessage(DISMISSPROGRESS);
        }).start();
    }

    public void showToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean isPreparedToBuildSmali = false;

    @Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.wait));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        return dialog;
    }

    public static void showMessage(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNeutralButton(R.string.btn_ok, null);
        builder.show();
    }

    @SuppressLint("MissingPermission")
    public void SystemInfo() {
        StringBuilder info = new StringBuilder();
        String pei = MiscellaneousFunctions.createUUID(this);
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String dui = new UUID(androidId.hashCode(),
                ((long) (pei.hashCode() << 64) ^ Math.abs((long)pei.hashCode())) & (long)MiscellaneousFunctions.getDeviceId(this).hashCode()).toString();
        dui = dui.replaceAll("-", "");
        info.append("Phone model：" + Build.MODEL + "\n");
        info.append("Manufacturer：" + Build.MANUFACTURER + "\n");
        info.append("Android version：" + Build.VERSION.RELEASE + "\n");
        info.append("Android SDK code：" + Build.VERSION.SDK_INT + "\n");
        info.append("CPU variant：" + Build.CPU_ABI + " / " + Build.CPU_ABI2 + "\n");
        info.append("Hardware serial code：" + Build.SERIAL + "\n");
        info.append("Hardware name：" + Build.HARDWARE + "\n");
        info.append("Baseband version：" + Build.getRadioVersion() + "\n");
        info.append("BootLoader version：" + Build.BOOTLOADER + "\n");
        info.append("Device ID：" + MiscellaneousFunctions.getDeviceId(this) + "\n");
        info.append("Machine code：" + dui.hashCode() + "\n");
        info.append("App signature：" + Features.compressStrToInt(getPkgSign(this)));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.system_info));
        builder.setMessage(info.toString());
        builder.setNeutralButton(R.string.btn_ok, null);
        builder.show();
    }

    public String readAboutContent() {
        StringBuilder changelog = new StringBuilder();
        Locale locale = getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        String filename;
        switch (language) {
            case "en":
            default:
                filename = "changelog-en.txt";
                break;
            case "es":
                filename = "changelog-es.txt";
                break;
            case "fr":
                filename = "changelog-fr.txt";
                break;
            case "zh":
                filename = "changelog-zh.txt";
                break;
        }
        try {
            InputStream inputStream = getAssets().open(filename);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                changelog.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return changelog.toString();
    }

    public void showAbout() {
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.android);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(new BitmapDrawable(bmp));
        String title = getString(R.string.app_name);
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            if(pi.versionName != null)
                title += " " + pi.versionName;
        } catch(Exception e) {
            e.printStackTrace();
        }
        builder.setTitle(title);
        builder.setMessage(readAboutContent());
        builder.setNeutralButton(R.string.btn_ok, null);
        builder.setPositiveButton(R.string.system_info, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                SystemInfo();
            }
        });
        builder.show();
    }

    public static void prompt(Context context, String title, String message,
                              DialogInterface.OnClickListener btnlisten) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.btn_ok, btnlisten);
        builder.setNegativeButton(R.string.btn_cancel, btnlisten);
        builder.show();
    }

    private void delete(final File file) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.delete);
        alert.setMessage(String.format(getString(R.string.is_delete), file.getName()));
        alert.setPositiveButton(R.string.btn_yes, (dialog, whichButton) -> new Thread(() -> {
            mHandler.sendEmptyMessage(SHOWPROGRESS);
            FileUtil.delete(file);
            mFileList.remove(file);
            Message msg = new Message();
            msg.what = TOAST;
            msg.obj = file.getName() + getString(R.string.deleted);
            mHandler.sendMessage(msg);
            mHandler.sendEmptyMessage(DISMISSPROGRESS);
        }).start());
        alert.setNegativeButton(R.string.btn_no, null);
        alert.show();
    }

    private void newFolder() {
        final EditText folderName = new EditText(this);
        folderName.setHint(R.string.folder_name);
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.add_folder);
        alert.setView(folderName);
        alert.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = folderName.getText().toString();
                if(name.length() == 0) {
                    toast(getString(R.string.directory_empty));
                    return;
                } else {
                    for(File f : mFileList) {
                        if(f.getName().equals(name)) {
                            toast(String.format(getString(R.string.directory_exists, name)));
                            return;
                        }
                    }
                }
                File dir = new File(mCurrentDir, name);
                if(!dir.mkdirs())
                    toast(String.format(getString(R.string.directory_cannot_create), name));
                else
                    toast(String.format(getString(R.string.directory_created), name));
                mAdapter.notifyDataSetInvalidated();
            }
        });
        alert.setNegativeButton(R.string.btn_cancel, null);
        alert.show();
    }

    private void rename(final File file) {
        final EditText newName = new EditText(this);
        newName.setText(file.getName());
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.rename);
        alert.setView(newName);
        alert.setPositiveButton(R.string.btn_ok, (dialog, whichButton) -> {
            String name = newName.getText().toString();
            if(name.length() == 0) {
                toast(getString(R.string.name_empty));
                return;
            } else {
                for(File f : mFileList) {
                    if(f.getName().equals(name)) {
                        toast(String.format(getString(R.string.file_exists), name));
                        return;
                    }
                }
            }
            if(!FileUtil.rename(file, name))
                toast(String.format(getString(R.string.cannot_rename), file.getPath()));
            mAdapter.notifyDataSetInvalidated();
        });
        alert.setNegativeButton(R.string.btn_cancel, null);
        alert.show();
    }

    public interface ImageCallback {
        void imageLoaded(Drawable imageDrawable, ImageView imageView);
    }

}
