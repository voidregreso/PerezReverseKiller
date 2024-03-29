package com.perez.revkiller;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ScrollView;
import android.widget.CheckBox;
import android.util.Log;
import android.database.DataSetObserver;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Stack;

import org.jb.dexlib.*;
import org.jb.dexlib.Util.*;

import com.perez.code.DalvikParser;

public class ClassListActivity extends AppCompatActivity {

    public static String searchString = "";
    public static String searchFieldClass = "";
    public static String searchFieldName = "";
    public static String searchFieldDescriptor = "";
    public static String searchMethodClass = "";
    public static String searchMethodName = "";
    public static String searchMethodDescriptor = "";
    public static final int SAVEFILE = 1;
    public static final int SAVEDISMISS = 2;
    private static final String title = "/";
    public Tree<HashMap<String, ClassDefItem>> tree;

    public static HashMap<String, ClassDefItem> classMap;
    public static HashMap<String, ClassDefItem> deleteclassMap;
    public static DexFile dexFile;
    public static boolean isChanged;
    public static ClassDefItem curClassDef;

    public static String curFile;
    private ClassListAdapter mAdapter;
    private List<String> classList;

    private int mod;

    private static final int OPENDIR = 10;
    private static final int BACK = 11;
    private static final int UPDATE = 12;
    private static final int INIT = 13;
    private static final int TOAST = 14;
    private static final int SEARCH = 15;
    private static final int SEARCHDISMISS = 16;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case SAVEFILE:
                ClassListActivity.this.showDialog(SAVEFILE);
                break;
            case SEARCH:
                ClassListActivity.this.showDialog(SEARCH);
                break;
            case SAVEDISMISS:
                ClassListActivity.this.dismissDialog(SAVEFILE);
                break;
            case SEARCHDISMISS:
                ClassListActivity.this.dismissDialog(SEARCH);
                break;
            case TOAST:
                toast(msg.obj.toString());
                break;
            }
        }
    };

    public ListView lv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.class_list);
        lv = findViewById(R.id.clslist);
        init();
        mAdapter = new ClassListAdapter(this);
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onInvalidated() {
                switch(mod) {
                case OPENDIR:
                    tree.push(curFile);
                    classList = tree.list();
                    break;
                case BACK:
                    tree.pop();
                    classList = tree.list();
                    break;
                case UPDATE:
                    classList = tree.list();
                    break;
                case INIT:
                    init();
                    break;
                }
                setTitle(title + tree.getCurPath());
            }
        });
        lv.setAdapter(mAdapter);
        registerForContextMenu(lv);
        lv.setOnItemClickListener((list, view, position, id) -> {
            curFile = (String) list.getItemAtPosition(position);
            if(tree.isDirectory(curFile)) {
                mod = OPENDIR;
                mAdapter.notifyDataSetInvalidated();
                return;
            }
            curClassDef = classMap.get(tree.getCurPath() + curFile);
            Intent intent = new Intent(ClassListActivity.this, ClassItemActivity.class);
            startActivity(intent);
        });
        Button btn = findViewById(R.id.btn_string_pool);
        btn.setOnClickListener(v -> openStringPool());
    }

    private void init() {
        if(classMap == null)
            classMap = new HashMap<>();
        else
            classMap.clear();
        HashMap<String, ClassDefItem> classMap = ClassListActivity.classMap;
        HashMap<String, ClassDefItem> deleteclassMap = ClassListActivity.deleteclassMap;
        for(ClassDefItem classItem : dexFile.ClassDefsSection.getItems()) {
            String className = classItem.getClassType().getTypeDescriptor();
            className = className.substring(1, className.length() - 1);
            if(deleteclassMap != null && deleteclassMap.get(className) != null)
                continue;
            classMap.put(className, classItem);
        }
        tree = new Tree(classMap.keySet(), classMap);
        setTitle(title + tree.getCurPath());
        classList = tree.list();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        MenuInflater in = getMenuInflater();
        in.inflate(R.menu.class_list_menu, m);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mi) {
        int id = mi.getItemId();
        switch(id) {
        case R.id.save_dexfile:
            new Thread(() -> {
                mHandler.sendEmptyMessage(SAVEFILE);
                saveDexFile();
                mHandler.sendEmptyMessage(SAVEDISMISS);
                setResultToZipEditor();
            }).start();
            break;
        case R.id.search_string:
            searchString();
            break;
        case R.id.search_method:
            searchMethod();
            break;
        case R.id.search_field:
            searchField();
            break;
        case R.id.merger_dexfile:
            selectDexFile();
            break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        menu.add(Menu.NONE, R.string.rename_class, Menu.NONE,
                 R.string.rename_class);
        menu.add(Menu.NONE, R.string.remove_class, Menu.NONE,
                 R.string.remove_class);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case ActResConstant.class_list_item:
            switch(resultCode) {
            case ActResConstant.add_entry:
                if(mergerDexFile(data.getStringExtra(PerezReverseKillerMain.ENTRYPATH)))
                    toast(getString(R.string.dex_merged));
                break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(this);
        switch(id) {
        case SAVEFILE:
            dialog.setMessage(getString(R.string.saving));
            break;
        case SEARCH:
            dialog.setMessage(getString(R.string.searching));
            break;
        }
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        return dialog;
    }

    public static void setCurrnetClass(String className) {
        curClassDef = classMap.get(className);
    }

    private void searchString() {
        LayoutInflater inflate = getLayoutInflater();
        ScrollView scroll = (ScrollView) inflate.inflate(
                                R.layout.alert_dialog_search_string, null);
        final EditText srcName = (EditText) scroll.findViewById(R.id.src_edit);
        srcName.setText(searchString);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.search_string);
        alert.setView(scroll);
        alert.setPositiveButton(R.string.btn_ok, (dialog, whichButton) -> {
            searchString = srcName.getText().toString();
            if(searchString.length() == 0) {
                toast(getString(R.string.search_name_empty));
                return;
            }
            new Thread(() -> {
                mHandler.sendEmptyMessage(SEARCH);
                List<String> classList = new ArrayList<String>();
                searchStringInMethods(classList, searchString);
                SearchClassesActivity.initClassList(classList);
                mHandler.sendEmptyMessage(SEARCHDISMISS);
                sendIntentToSearchActivity();
            }).start();
        });
        alert.setNegativeButton(R.string.btn_cancel, null);
        alert.show();
    }

    private void searchField() {
        LayoutInflater inflate = getLayoutInflater();
        ScrollView scroll = (ScrollView) inflate.inflate(
                                R.layout.alert_dialog_search_field, null);
        final EditText fieldClass = (EditText) scroll
                                    .findViewById(R.id.class_edit);
        final CheckBox ignoreNameAndDescriptor = (CheckBox) scroll
                .findViewById(R.id.ignore_name_descriptor);
        final EditText fieldName = (EditText) scroll
                                   .findViewById(R.id.name_edit);
        final CheckBox ignoreDescriptor = (CheckBox) scroll
                                          .findViewById(R.id.ignore_descriptor);
        final EditText fieldDescriptor = (EditText) scroll
                                         .findViewById(R.id.descriptor_edit);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.search_field);
        alert.setView(scroll);
        fieldClass.setText(searchFieldClass);
        fieldName.setText(searchFieldName);
        fieldDescriptor.setText(searchFieldDescriptor);
        alert.setPositiveButton(R.string.btn_ok,
                (dialog, whichButton) -> new Thread(() -> {
                    mHandler.sendEmptyMessage(SEARCH);
                    searchFieldClass = fieldClass.getText()
                                       .toString();
                    searchFieldName = fieldName.getText()
                                      .toString();
                    searchFieldDescriptor = fieldDescriptor
                                            .getText().toString();
                    List<String> classList = new ArrayList<>();
                    searchFieldInMethods(classList,
                                         searchFieldClass, searchFieldName,
                                         searchFieldDescriptor,
                                         ignoreNameAndDescriptor.isChecked(),
                                         ignoreDescriptor.isChecked());
                    SearchClassesActivity.initClassList(classList);
                    mHandler.sendEmptyMessage(SEARCHDISMISS);
                    sendIntentToSearchActivity();
                }).start());
        alert.setNegativeButton(R.string.btn_cancel, null);
        alert.show();
    }

    private void searchMethod() {
        LayoutInflater inflate = getLayoutInflater();
        ScrollView scroll = (ScrollView) inflate.inflate(
                                R.layout.alert_dialog_search_method, null);
        final EditText methodClass = (EditText) scroll
                                     .findViewById(R.id.class_edit);
        final CheckBox ignoreNameAndDescriptor = (CheckBox) scroll
                .findViewById(R.id.ignore_name_descriptor);
        final EditText methodName = (EditText) scroll
                                    .findViewById(R.id.name_edit);
        final CheckBox ignoreDescriptor = (CheckBox) scroll
                                          .findViewById(R.id.ignore_descriptor);
        final EditText methodDescriptor = (EditText) scroll
                                          .findViewById(R.id.descriptor_edit);
        methodClass.setText(searchMethodClass);
        methodName.setText(searchMethodName);
        methodDescriptor.setText(searchMethodDescriptor);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.search_method);
        alert.setView(scroll);
        alert.setPositiveButton(R.string.btn_ok,
                (dialog, whichButton) -> {
                    searchMethodClass = methodClass.getText().toString();
                    searchMethodName = methodName.getText().toString();
                    searchMethodDescriptor = methodDescriptor.getText()
                                             .toString();
                    List<String> classList = new ArrayList<String>();
                    searchMethodInMethods(classList, searchMethodClass,
                                          searchMethodName, searchMethodDescriptor,
                                          ignoreNameAndDescriptor.isChecked(),
                                          ignoreDescriptor.isChecked());
                    SearchClassesActivity.initClassList(classList);
                    sendIntentToSearchActivity();
                });
        alert.setNegativeButton(R.string.btn_cancel, null);
        alert.show();
    }

    private void sendIntentToSearchActivity() {
        Intent intent = new Intent(ClassListActivity.this,
                                   SearchClassesActivity.class);
        startActivity(intent);
    }

    private void clearAll() {
        if(classMap != null)
            classMap.clear();
        classMap = null;
        deleteclassMap = null;
        dexFile = null;
        curClassDef = null;
        tree = null;
        curFile = null;
        isChanged = false;
        System.gc();
    }

    private void saveDexFile() {
        DexFile outDexFile = new DexFile();
        HashMap<String, ClassDefItem> classMap = ClassListActivity.classMap;
        HashMap<String, ClassDefItem> deleteclassMap = ClassListActivity.deleteclassMap;
        for(Map.Entry<String, ClassDefItem> entry : classMap.entrySet()) {
            if(deleteclassMap != null
                    && deleteclassMap.get(entry.getKey()) != null)
                continue;
            ClassDefItem classDef = entry.getValue();
            classDef.internClassDefItem(outDexFile);
        }
        outDexFile.setSortAllItems(true);
        outDexFile.place();

        byte[] buf = new byte[outDexFile.getFileSize()];
        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(buf);
        outDexFile.writeTo(out);
        DexFile.calcSignature(buf);
        DexFile.calcChecksum(buf);
        TextEditor.data = buf;
        outDexFile = null;
        isChanged = false;
    }

    private boolean mergerDexFile(String name) {
        try {
            DexFile tmp = new DexFile(name);
            DexFile dexFile = ClassListActivity.dexFile;
            IndexedSection<ClassDefItem> classes = tmp.ClassDefsSection;
            List<ClassDefItem> classDefList = classes.getItems();
            for(ClassDefItem classDef : classDefList) {
                String className = classDef.getClassType().getTypeDescriptor();
                className = className.substring(1, className.length() - 1);
                if(deleteclassMap != null)
                    deleteclassMap.put(className, null);
                classDef.internClassDefItem(dexFile);
            }
            mod = INIT;
            mAdapter.notifyDataSetInvalidated();
            isChanged = true;
        } catch(Exception e) {
            PerezReverseKillerMain.showMessage(this, "Open dexFile exception",
                                        e.getMessage());
            return false;
        }
        System.gc();
        return true;
    }

    private void openStringPool() {
        Intent intent = new Intent(this, TextEditor.class);
        intent.putExtra(TextEditor.PLUGIN, "StringIdsEditor");
        startActivity(intent);
    }

    private void replaceClassType(String src, String dst) {
        for(TypeIdItem type : dexFile.TypeIdsSection.getItems()) {
            String s = type.getTypeDescriptor();

            int pos = 1;
            for(int i = 0; i < s.length(); i++) {
                if(s.charAt(i) != '[')
                    break;
                pos++;
            }
            int i = s.indexOf(src);
            if(i != -1 && i == pos) {
                s = s.replace(src, dst);
                type.setTypeDescriptor(s);
            }
        }
    }

    private void renameType(final String className) {
        final EditText newName = new EditText(this);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final boolean isDirectory = className.endsWith("/");
        if(isDirectory)
            newName.setText(className.substring(0, className.length() - 1));
        else
            newName.setText(className);
        alert.setTitle(R.string.rename);
        alert.setView(newName);
        alert.setPositiveButton(R.string.btn_ok,
                (dialog, whichButton) -> {
                    String name = newName.getText().toString();
                    if(name.length() == 0 || name.indexOf("/") != -1) {
                        toast(getString(R.string.name_empty));
                        return;
                    } else {
                        for(String s : classList) {
                            if(s.equals(name)) {
                                toast(String.format(
                                          getString(R.string.class_exists),
                                          name));
                                return;
                            }
                        }
                    }
                    name += isDirectory ? "/" : "";
                    String cur = tree.getCurPath();
                    replaceClassType(cur + className, cur + name);
                    isChanged = true;
                    mod = INIT;
                    mAdapter.notifyDataSetInvalidated();
                });
        alert.setNegativeButton(R.string.btn_cancel, null);
        alert.show();
    }

    private void selectDexFile() {
        Intent intent = new Intent(this, PerezReverseKillerMain.class);
        intent.putExtra(PerezReverseKillerMain.SELECTEDMOD, true);
        startActivityForResult(intent, ActResConstant.class_list_item);
    }

    private static void searchStringInMethods(List<String> list, String src) {
        HashMap<String, ClassDefItem> classMap = ClassListActivity.classMap;
        HashMap<String, ClassDefItem> deleteclassMap = ClassListActivity.deleteclassMap;
        for(Map.Entry<String, ClassDefItem> entry : classMap.entrySet()) {
            if(deleteclassMap != null
                    && deleteclassMap.get(entry.getKey()) != null)
                continue;
            ClassDefItem classItem = entry.getValue();
            boolean isSearch = false;
            ClassDataItem classData = classItem.getClassData();
            if(classData != null) {

                ClassDataItem.EncodedMethod[] methods = classData
                                                        .getDirectMethods();
                for(ClassDataItem.EncodedMethod method : methods) {
                    if(DalvikParser.searchStringInMethod(method, src)) {
                        String name = classItem.getClassType()
                                      .getTypeDescriptor();
                        list.add(name.substring(1, name.length() - 1));
                        isSearch = true;
                        break;
                    }
                }
                if(isSearch)
                    continue;

                methods = classData.getVirtualMethods();
                for(ClassDataItem.EncodedMethod method : methods) {
                    if(DalvikParser.searchStringInMethod(method, src)) {
                        String name = classItem.getClassType()
                                      .getTypeDescriptor();
                        list.add(name.substring(1, name.length() - 1));
                        break;
                    }
                }
            }
        }
    }

    private static void searchFieldInMethods(List<String> list,
            String classType, String name, String descriptor,
            boolean ignoreNameAndDescriptor, boolean ignoreDescriptor) {
        HashMap<String, ClassDefItem> classMap = ClassListActivity.classMap;
        HashMap<String, ClassDefItem> deleteclassMap = ClassListActivity.deleteclassMap;
        for(Map.Entry<String, ClassDefItem> entry : classMap.entrySet()) {
            if(deleteclassMap != null
                    && deleteclassMap.get(entry.getKey()) != null)
                continue;
            ClassDefItem classItem = entry.getValue();
            boolean isSearch = false;
            ClassDataItem classData = classItem.getClassData();
            if(classData != null) {

                ClassDataItem.EncodedMethod[] methods = classData
                                                        .getDirectMethods();
                for(ClassDataItem.EncodedMethod method : methods) {
                    if(DalvikParser.searchFieldInMethod(method, classType, name,
                                                  descriptor, ignoreNameAndDescriptor,
                                                  ignoreDescriptor)) {
                        String string = classItem.getClassType()
                                        .getTypeDescriptor();
                        list.add(string.substring(1, string.length() - 1));
                        isSearch = true;
                        break;
                    }
                }
                if(isSearch)
                    continue;

                methods = classData.getVirtualMethods();
                for(ClassDataItem.EncodedMethod method : methods) {
                    if(DalvikParser.searchFieldInMethod(method, classType, name,
                                                  descriptor, ignoreNameAndDescriptor,
                                                  ignoreDescriptor)) {
                        String string = classItem.getClassType()
                                        .getTypeDescriptor();
                        list.add(string.substring(1, string.length() - 1));
                        break;
                    }
                }
            }
        }
    }

    private static void searchMethodInMethods(List<String> list,
            String classType, String name, String descriptor,
            boolean ignoreNameAndDescriptor, boolean ignoreDescriptor) {
        HashMap<String, ClassDefItem> classMap = ClassListActivity.classMap;
        HashMap<String, ClassDefItem> delClassMap = ClassListActivity.deleteclassMap;
        for(Map.Entry<String, ClassDefItem> entry : classMap.entrySet()) {
            if(delClassMap != null
                    && delClassMap.get(entry.getKey()) != null)
                continue;
            ClassDefItem classItem = entry.getValue();
            boolean isSearch = false;
            ClassDataItem classData = classItem.getClassData();
            if(classData != null) {

                ClassDataItem.EncodedMethod[] methods = classData
                                                        .getDirectMethods();
                for(ClassDataItem.EncodedMethod method : methods) {
                    if(DalvikParser.searchMethodInMethod(method, classType, name,
                                                   descriptor, ignoreNameAndDescriptor,
                                                   ignoreDescriptor)) {
                        String string = classItem.getClassType()
                                        .getTypeDescriptor();
                        list.add(string.substring(1, string.length() - 1));
                        isSearch = true;
                        break;
                    }
                }
                if(isSearch)
                    continue;

                methods = classData.getVirtualMethods();
                for(ClassDataItem.EncodedMethod method : methods) {
                    if(DalvikParser.searchMethodInMethod(method, classType, name,
                                                   descriptor, ignoreNameAndDescriptor,
                                                   ignoreDescriptor)) {
                        String string = classItem.getClassType()
                                        .getTypeDescriptor();
                        list.add(string.substring(1, string.length() - 1));
                        break;
                    }
                }
            }
        }
    }

    private void showDialogIfChanged() {
        if(isChanged) {
            PerezReverseKillerMain.prompt(this, getString(R.string.prompt),
                                   getString(R.string.is_save),
                    (dailog, which) -> {
                        if(which == AlertDialog.BUTTON_POSITIVE) {
                            new Thread(() -> {
                                mHandler.sendEmptyMessage(SAVEFILE);
                                saveDexFile();
                                mHandler.sendEmptyMessage(SAVEDISMISS);
                                setResultToZipEditor();
                            }).start();
                        } else if(which == AlertDialog.BUTTON_NEGATIVE)
                            finish();
                    });
        } else
            finish();
    }

    private void setResultToZipEditor() {
        Intent intent = getIntent();
        setResult(ActResConstant.text_editor, intent);
        finish();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch(ClassCastException e) {
            Log.e(e.toString(), "Bad menuInfo");
            return false;
        }
        switch(item.getItemId()) {
        case R.string.rename_class: {
            String className = classList.get(info.position);
            renameType(className);
        }
        break;
        case R.string.remove_class:
            final String name = classList.get(info.position);
            PerezReverseKillerMain.prompt(this, getString(R.string.is_remove), name,
                    (dialog, which) -> {
                        if(which == AlertDialog.BUTTON_POSITIVE) {
                            if(tree.isDirectory(name))
                                removeClassesDir(name);
                            else
                                removeClasses(name);
                        }
                    });
            break;
        }
        return true;
    }

    private void removeClassesDir(String name) {
        if(deleteclassMap == null)
            deleteclassMap = new HashMap<>();
        HashMap<String, ClassDefItem> deleteclassMap = ClassListActivity.deleteclassMap;
        String cur = tree.getCurPath() + name;
        for(String key : classMap.keySet()) {
            if(key.indexOf(cur) == 0)
                deleteclassMap.put(key, classMap.get(key));
        }
        isChanged = true;
        mod = INIT;
        mAdapter.notifyDataSetInvalidated();
    }

    private void removeClasses(String name) {
        if(deleteclassMap == null)
            deleteclassMap = new HashMap<>();
        String cur = tree.getCurPath() + name;
        deleteclassMap.put(cur, classMap.get(cur));
        isChanged = true;
        mod = INIT;
        mAdapter.notifyDataSetInvalidated();
    }

    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearAll();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(!getTitle().equals(title)) {
                mod = BACK;
                mAdapter.notifyDataSetInvalidated();
                return true;
            } else {
                showDialogIfChanged();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private class ClassListAdapter extends BaseAdapter {

        protected final Context mContext;
        protected final LayoutInflater mInflater;
        LinearLayout container;

        public ClassListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return classList.size();
        }

        public Object getItem(int position) {
            return classList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            String file = classList.get(position);
            if(convertView == null) {
                container = (LinearLayout) mInflater.inflate(
                                R.layout.class_list_item, null);
            } else
                container = (LinearLayout) convertView;
            ImageView icon = (ImageView) container
                             .findViewById(R.id.list_item_icon);
            if(tree.isDirectory(file))
                icon.setImageResource(R.drawable.folder);
            else
                icon.setImageResource(R.drawable.clazz);
            TextView text = (TextView) container
                            .findViewById(R.id.list_item_title);
            text.setText(file);
            return container;
        }
    }

}
