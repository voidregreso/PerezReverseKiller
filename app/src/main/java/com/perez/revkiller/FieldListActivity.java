package com.perez.revkiller;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jb.dexlib.ClassDataItem;
import org.jb.dexlib.ClassDataItem.EncodedField;
import org.jb.dexlib.ClassDefItem;

import java.util.ArrayList;
import java.util.List;

public class FieldListActivity extends AppCompatActivity {

    private FieldListAdapter mAdapter;
    private List<String> fieldList;
    private List<String> fieldDescriptor;
    private ClassDefItem classDef;
    private int staticFieldsCount;
    public static boolean isStaticField = true;
    public static int fieldIndex;
    private int listPos;
    public ListView lv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listact);
        init();
        lv = findViewById(R.id.file_list_view);
        mAdapter = new FieldListAdapter(this);
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onInvalidated() {
                init();
            }
        });
        lv.setAdapter(mAdapter);
        registerForContextMenu(lv);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> list, View view, int position, long id) {
                if(position < staticFieldsCount) {
                    isStaticField = true;
                    fieldIndex = position;
                } else {
                    isStaticField = false;
                    fieldIndex = position - staticFieldsCount;
                }
                listPos = list.getFirstVisiblePosition();
                Intent intent = new Intent(FieldListActivity.this, FieldItemEditorActivity.class);
                startActivityForResult(intent, ActResConstant.field_list_item);
            }
        });
    }

    private void init() {
        if(fieldList == null)
            fieldList = new ArrayList<String>();
        else
            fieldList.clear();
        if(fieldDescriptor == null)
            fieldDescriptor = new ArrayList<String>();
        else
            fieldDescriptor.clear();
        classDef = ClassListActivity.curClassDef;
        ClassDataItem classData = classDef.getClassData();
        if(classData != null) {
            EncodedField[] staticFields = classData.getStaticFields();
            staticFieldsCount = staticFields.length;
            EncodedField[] instanceFields = classData.getInstanceFields();
            for(EncodedField field : staticFields) {
                fieldList.add(field.field.getFieldName().getStringValue());
                fieldDescriptor.add(field.field.getFieldType()
                                    .getTypeDescriptor());
            }
            for(EncodedField field : instanceFields) {
                fieldList.add(field.field.getFieldName().getStringValue());
                fieldDescriptor.add(field.field.getFieldType()
                                    .getTypeDescriptor());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, R.string.add_field, Menu.NONE, R.string.add_field);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mi) {
        int id = mi.getItemId();
        switch(id) {
        case R.string.add_field:
            Intent intent = new Intent(this, FieldItemNewActivity.class);
            startActivityForResult(intent, ActResConstant.field_list_item);
            break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        menu.add(Menu.NONE, R.string.remove_field, Menu.NONE,
                 R.string.remove_field);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case ActResConstant.field_list_item:
            switch(resultCode) {
            case ActResConstant.add_field:
            case ActResConstant.field_item_editor:
                mAdapter.notifyDataSetInvalidated();
                lv.setSelection(listPos);
                break;
            }
        }
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
        int position = info.position;
        switch(item.getItemId()) {
        case R.string.remove_field:
            ClassDataItem classData = classDef.getClassData();
            if(position < staticFieldsCount)
                classData.removeStaticField(position);
            else
                classData.removeInstanceField(position - staticFieldsCount);
            mAdapter.notifyDataSetInvalidated();
            break;
        }
        return true;
    }

    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clearAll();
    }

    private void clearAll() {
        classDef = null;
        fieldList = null;
        fieldDescriptor = null;
        mAdapter = null;
        System.gc();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(classDef.getClassData() != null)
                classDef.getClassData().sortFields();
        }
        return super.onKeyDown(keyCode, event);
    }

    private class FieldListAdapter extends BaseAdapter {

        protected final Context mContext;
        protected final LayoutInflater mInflater;

        public FieldListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return fieldList.size();
        }

        public Object getItem(int position) {
            return fieldList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            RelativeLayout container;
            if(convertView == null) {
                container = (RelativeLayout) mInflater.inflate(
                                R.layout.field_list_item, null);
            } else
                container = (RelativeLayout) convertView;
            ImageView icon = (ImageView) container.findViewById(R.id.icon);
            icon.setImageResource(R.drawable.field);
            TextView text = (TextView) container.findViewById(R.id.text);
            text.setText(fieldList.get(position));
            TextView descriptor = (TextView) container
                                  .findViewById(R.id.descriptor);
            descriptor.setText(fieldDescriptor.get(position));
            return container;
        }
    }

}
