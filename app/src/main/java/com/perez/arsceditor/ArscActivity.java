package com.perez.arsceditor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.perez.arsceditor.ResDecoder.ARSCCallBack;
import com.perez.arsceditor.ResDecoder.data.ResTable;
import com.perez.arsceditor.Translate.DoTranslate;
import com.perez.revkiller.R;
import androidx.appcompat.app.AppCompatActivity;

@SuppressWarnings("deprecation")
public class ArscActivity extends AppCompatActivity implements OnItemLongClickListener {

    
    public List<String> txtOriginal = new ArrayList<String>();
    
    public List<String> txtTranslated = new ArrayList<String>();
    
    public List<String> txtTranslated_Key = new ArrayList<String>();
    
    public ListView stringListView;
    
    public stringListAdapter mAdapter;
    
    public static List<String> Configs;
    
    public static List<String> Types;
    
    private List<ContentValues> RESOURCES = new ArrayList<ContentValues>();
    
    private TextView textCategory;
    
    private TextView textConfig;
    
    private TextView info;
    
    private ImageView btnTranslate;
    
    private ImageView btnSearch;
    
    private ImageView btnSave;
    
    private AndrolibResources mAndRes;
    
    private String fp;
    
    public boolean isChanged = false;
    
    private int ResType;
    
    public static final int ARSC = 0, AXML = 1, DEX = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.zgstring_list);
        
        stringListView = (ListView) findViewById(R.id.list_res_string);
        
        textCategory = (TextView) findViewById(R.id.textCategory);
        
        textConfig = (TextView) findViewById(R.id.textConfig);
        
        btnTranslate = (ImageView) findViewById(R.id.btnTranslate);
        
        btnSearch = (ImageView) findViewById(R.id.btnSearch);
        
        btnSave = (ImageView) findViewById(R.id.btnSave);
        
        info = (TextView) findViewById(R.id.info);
        
        textCategory.setOnClickListener(MyOnClickListener);
        
        textConfig.setOnClickListener(MyOnClickListener);
        
        textCategory.addTextChangedListener(textWatcher);
        
        textConfig.addTextChangedListener(textWatcher);
        
        btnTranslate.setOnClickListener(MyOnClickListener);
        
        btnSearch.setOnClickListener(MyOnClickListener);
        
        btnSave.setOnClickListener(MyOnClickListener);
        
        mAdapter = new stringListAdapter(this);
        
        stringListView.setAdapter(mAdapter);
        
        stringListView.setOnItemLongClickListener(this);
        try {
            fp = getIntent().getStringExtra("FilePath");
            open(fp);
        } catch(IOException e) {
            System.err.println(fp);
            e.printStackTrace();
            finish();
        }
    }

    
    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int arg2, long arg3) {
        if(textCategory.getText().toString().equals("id"))
            return false;
        
        new AlertDialog.Builder(this).setItems(R.array.translate, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                
                DoTranslate translateTask = new DoTranslate(txtOriginal, txtTranslated, false, ArscActivity.this);
                
                translateTask.init(arg2);
            }
        }).create().show();
        return true;
    }

    
    private OnClickListener MyOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub
            switch(arg0.getId()) {
            
            case R.id.btnTranslate:
                if(textCategory.getText().toString().equals("id")) {
                    Toast.makeText(ArscActivity.this, R.string.can_not_edit, Toast.LENGTH_LONG).show();
                    return;
                }
                
                DoTranslate translate = new DoTranslate(txtOriginal, txtTranslated, true, ArscActivity.this);
                
                translate.init(0);
                break;
            
            case R.id.btnSearch:
                
                SearchString searchTask = new SearchString(ArscActivity.this, txtOriginal);
                
                searchTask.search();
                break;
            
            case R.id.btnSave:
                
                SaveFileTask saveTask = new SaveFileTask();
                
                saveTask.execute(fp);
                break;
            
            case R.id.textCategory:
                
                new AlertDialog.Builder(ArscActivity.this).setTitle("")
                .setItems((String[]) Types.toArray(new String[Types.size()]),
                new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        textCategory.setText(Types.get(arg1));
                    }
                })
                .create().show();
                break;
            
            case R.id.textConfig:
                new AlertDialog.Builder(ArscActivity.this).setTitle("")
                .setItems((String[]) Configs.toArray(new String[Configs.size()]),
                new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        textConfig.setText(Configs.get(arg1));
                    }
                })
                .create().show();
                break;
            }
        }
    };

    private void open(String resFile) throws IOException {
        if(resFile.endsWith(".arsc"))
            open(new FileInputStream(resFile), ARSC);
        else if(resFile.endsWith(".xml"))
            open(new FileInputStream(resFile), AXML);
        else if(resFile.endsWith(".dex"))
            open(new FileInputStream(resFile), DEX);
        else
            throw new IOException("Unsupported FileType");
    }

    private void open(InputStream resInputStream, int resType) {
        
        AsyncTask<InputStream, Integer, String> task = new ParseTask();
        try {
            
            task.execute(resInputStream);
            ResType = resType;
        } catch(OutOfMemoryError e) {
            showMessage(this, getString(R.string.out_of_memory)).show();
        }
        
        AsyncTask<String, Integer, String> getTask = new GetTask();
        
        getTask.execute(textCategory.getText().toString(), textConfig.getText().toString());
    }

    /**
     * 
     *
     * @author com.perezhai
     */
    private TextWatcher textWatcher = new TextWatcher() {
        
        @Override
        public void afterTextChanged(Editable s) {
            
            AsyncTask<String, Integer, String> task = new GetTask();
            
            task.execute(textCategory.getText().toString(), textConfig.getText().toString());
        }
        
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // TODO Auto-generated method stub
            Log.d("TAG", "beforeTextChanged--------------->");
        }
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

    /**
     * 
     *
     * @author com.perezhai
     */
    class GetTask extends AsyncTask<String, Integer, String> {
        
        private ProgressDialog dlg;

        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            
            dlg = new ProgressDialog(ArscActivity.this);
            
            dlg.setTitle(R.string.parsing);
            
            dlg.setCancelable(false);
            
            dlg.show();
            
            if(Configs == null) {
                
                Configs = new ArrayList<String>();
            }
            
            for(String str : txtTranslated) {
                if(!str.equals(""))
                    isChanged = true;
                break;
            }
            if(isChanged) {
                
                for(int i = 0; i < txtOriginal.size(); i++)
                    mAndRes.mARSCDecoder.mTableStrings.sortStringBlock(txtOriginal.get(i), txtTranslated.get(i));
            }
            
            txtOriginal.clear();
            txtTranslated.clear();
            txtTranslated_Key.clear();
            Configs.clear();
        }

        
        @Override
        protected String doInBackground(String... params) {
            switch(ResType) {
            case ARSC:
                for(ContentValues resource : RESOURCES) {
                    
                    String NAME = (String) resource.get(MyObj.NAME);
                    
                    String VALUE = (String) resource.get(MyObj.VALUE);
                    
                    String TYPE = (String) resource.get(MyObj.TYPE);
                    
                    String CONFIG = (String) resource.get(MyObj.CONFIG);
                    
                    if(CONFIG.startsWith("-") && !Configs.contains(CONFIG.substring(1)) && TYPE.equals(params[0]))
                        
                        Configs.add(CONFIG.substring(1));
                    
                    else if(!CONFIG.startsWith("-") && !Configs.contains(CONFIG) && TYPE.equals(params[0]))
                        Configs.add(CONFIG);
                    
                    if(TYPE.equals(params[0]) && CONFIG.startsWith("-") && CONFIG.substring(1).equals(params[1])) {
                        
                        txtOriginal.add(VALUE);
                        
                        txtTranslated.add("");
                        
                        txtTranslated_Key.add(NAME);
                        
                    } else if(TYPE.equals(params[0]) && !CONFIG.startsWith("-") && CONFIG.equals(params[1])) {
                        
                        txtOriginal.add(VALUE);
                        
                        txtTranslated.add("");
                        
                        txtTranslated_Key.add(NAME);
                    }
                }
                break;
            case AXML:
                try {
                    mAndRes.mAXMLDecoder.getStrings(txtOriginal);
                    for(int i = 0; i < txtOriginal.size(); i++) {
                        
                        txtTranslated.add("");
                        
                        txtTranslated_Key.add("");
                    }
                } catch(CharacterCodingException e) {
                    return e.toString();
                }
                break;
            case DEX:
                break;
            }
            
            return getString(R.string.success);
        }

        
        @Override
        protected void onPostExecute(String result) {
            
            dlg.dismiss();
            
            if(!result.equals(getString(R.string.success))) {
                
                showMessage(ArscActivity.this, result).show();
                return;
            } else if(result.equals(getString(R.string.success)) && txtOriginal.size() == 0) { 
                if(Configs.size() != 0)
                    
                    textConfig.setText(
                        Configs.contains("[DEFAULT]") ? Configs.get(Configs.indexOf("[DEFAULT]")) : Configs.get(0));
            }
            
            Collections.sort(Configs);
            //
            // Collections.sort(txtOriginal);
            
            mAdapter.notifyDataSetInvalidated();
        }

    }

    
    class MyObj {
        public final static String NAME = "name";
        public final static String VALUE = "value";
        public final static String TYPE = "type";
        public final static String CONFIG = "config";
    }

    /**
     * @author com.perezhai ARSC
     */
    class ParseTask extends AsyncTask<InputStream, Integer, String> {
        
        private ARSCCallBack callback;
        
        private ProgressDialog dlg;
        
        private ContentValues values = null;

        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            
            mAndRes = new AndrolibResources(ArscActivity.this);
            
            dlg = new ProgressDialog(ArscActivity.this);
            
            dlg.setTitle(R.string.parsing);
            
            dlg.setCancelable(false);
            
            dlg.show();
            
            if(Types == null) {
                
                Types = new ArrayList<String>();
            }
            
            callback = new ARSCCallBack() {
                
                int i = 0;
                @Override
                public void back(String config, String type, String key, String value) {
                    
                    if(type != null) {
                        values = new ContentValues();
                        values.put(MyObj.NAME, key);
                        values.put(MyObj.VALUE, value);
                        
                        values.put(MyObj.TYPE, type);
                        values.put(MyObj.CONFIG, config);
                        
                        RESOURCES.add(values);
                    }
                    
                    if(!Types.contains(type))
                        
                        Types.add(type);
                    
                    i++;
                    
                    publishProgress(i);
                }
            };
        }

        
        public ResTable getResTable(InputStream ARSCStream) throws IOException {
            return mAndRes.getResTable(ARSCStream);
        }

        
        @Override
        protected String doInBackground(InputStream... params) {
            try {
                switch(ResType) {
                case ARSC:
                    mAndRes.decodeARSC(getResTable(params[0]), callback);
                    break;
                case AXML:
                    mAndRes.decodeAXML(params[0]);
                    break;
                }
            } catch(Exception e) {
                return e.toString();
            }
            return getString(R.string.success);
        }

        
        @Override
        protected void onProgressUpdate(Integer... values) {
            dlg.setMessage(String.valueOf(values[0]));
        }

        
        @Override
        protected void onPostExecute(String result) {
            
            dlg.dismiss();
            
            if(!result.equals(getString(R.string.success))) {
                
                showMessage(ArscActivity.this, result).show();
                return;
            }
            
            Collections.sort(Types);
        }

    }

    /**
     * @author com.perezhai 
     */
    class SaveFileTask extends AsyncTask<String, String, String> {
        
        private ProgressDialog dlg;

        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            
            dlg = new ProgressDialog(ArscActivity.this);
            
            dlg.setTitle(R.string.saving);
            
            dlg.setCancelable(false);
            
            dlg.show();
        }

        
        @Override
        protected String doInBackground(String... params) {
            try {
                switch(ResType) {
                case ARSC:
                    
                    FileOutputStream fo1 = new FileOutputStream(params[0]);
                    mAndRes.mARSCDecoder.write(fo1, txtOriginal, txtTranslated);
                    fo1.close();
                    break;
                case AXML:
                    
                    FileOutputStream fo2 = new FileOutputStream(params[0]);
                    mAndRes.mAXMLDecoder.write(txtOriginal, txtTranslated, fo2);
                    fo2.close();
                    break;
                case DEX:
                    break;
                }
            } catch(IOException e) {
                return e.toString();
            } catch(OutOfMemoryError e) {
                return getString(R.string.out_of_memory);
            }
            return getString(R.string.success);
        }

        
        @Override
        protected void onPostExecute(String result) {
            
            dlg.dismiss();
            
            if(!result.equals(getString(R.string.success))) {
                
                showMessage(ArscActivity.this, result).show();
                return;
            }
            
            isChanged = false;
        }

    }

    
    public class stringListAdapter extends BaseAdapter {

        
        private Context mContext;
        
        private TextView txtOriginalView;
        
        private EditText txtTranslatedView;

        
        public stringListAdapter(Context context) {
            super();
            
            this.mContext = context;
        }

        
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return txtOriginal.size();
        }

        
        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return arg0;
        }

        
        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return arg0;
        }

        
        OnTouchListener touch = new OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if(arg1.getAction() == MotionEvent.ACTION_UP)
                    Toast.makeText(ArscActivity.this, R.string.can_not_edit, Toast.LENGTH_LONG).show();
                return false;
            }
        };
        
        @SuppressLint({ "ViewHolder", "InflateParams" })
        @Override
        public View getView(final int position, View view, ViewGroup arg2) {
            
            TextWatcher textWatcher = new TextWatcher() {
                
                @Override
                public void afterTextChanged(Editable s) {
                }
                
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // TODO Auto-generated method stub
                    Log.d("TAG", "beforeTextChanged--------------->");
                }
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    
                    txtTranslated.remove(position);
                    
                    txtTranslated.add(position, s.toString());
                }
            };
            
            view = LayoutInflater.from(mContext).inflate(R.layout.zgres_string_item, null);
            
            txtOriginalView = (TextView) view.findViewById(R.id.txtOriginal);
            
            txtTranslatedView = (EditText) view.findViewById(R.id.txtTranslated);
            
            if(textCategory.getText().toString().equals("style")) {
                
                stringListView.setVisibility(View.INVISIBLE);
                
                info.setVisibility(View.VISIBLE);
                
                info.setText(R.string.no_strings_for_editing);
                return view;
            } else {
                
                info.setVisibility(View.GONE);
                
                stringListView.setVisibility(View.VISIBLE);
            }
            
            if(textCategory.getText().toString().equals("id")) {
                txtTranslatedView.setFocusable(false);
                txtTranslatedView.setOnTouchListener(touch);
            }
            
            txtOriginalView.setText(txtOriginal.get(position));
            
            txtTranslatedView.setText(txtTranslated.get(position));
            
            txtTranslatedView.setHint(txtTranslated_Key.get(position));
            
            txtTranslatedView.addTextChangedListener(textWatcher);
            return view;
        }
    }

    
    public static AlertDialog.Builder showMessage(Context activity, String message) {
        return new AlertDialog.Builder(activity).setMessage(message).setNegativeButton(R.string.ok, null)
               .setCancelable(false).setTitle(R.string.error);
    }
}
