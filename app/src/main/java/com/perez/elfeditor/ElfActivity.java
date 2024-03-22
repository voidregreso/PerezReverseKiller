package com.perez.elfeditor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UnknownFormatConversionException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.perez.revkiller.R;
import android.view.*;

import androidx.appcompat.app.AppCompatActivity;

public class ElfActivity extends AppCompatActivity {

    /**
     * 
     *
     * @author com.perezhai
     */
    class GetTask extends AsyncTask<String, Void, Void> {
        
        private ProgressDialog dlg;

        
        @Override
        protected Void doInBackground(String... params) {
            if(RESOURCES != null) {
                ////////////////////////////////////////////////////////////////
                if(checkChanged()) {
                    if(textCategory.getText().toString().equals("dynstr"))
                        elfParser.sortStrData(txtOriginal, txtTranslated, elfParser.ro_items);
                    else {
                        elfParser.sortStrData(txtOriginal, txtTranslated, elfParser.dy_items);
                    }
                    isChanged = true;
                }
                ////////////////////////////////////////////////////////////
                txtOriginal.clear();
                txtTranslated.clear();
                for(ResourceHelper resource : RESOURCES.values()) {
                    
                    String VALUE = resource.VALUE;
                    
                    String TYPE = resource.TYPE;
                    if(TYPE.equals(params[0])) {
                        
                        txtOriginal.add(VALUE);
                    }
                }
                initList();
            }
            return null;
        }

        
        @Override
        protected void onPostExecute(Void result) {
            
            dlg.dismiss();
            
            mAdapter.notifyDataSetInvalidated();
        }

        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dlg = new ProgressDialog(ElfActivity.this);
            dlg.setCancelable(false);
            dlg.setTitle(R.string.parsing);
            dlg.show();
        }

    }

    class ParseTask extends AsyncTask<InputStream, Integer, String> {
        
        private ProgressDialog dlg;
        
        private ResourceCallBack callback;

        
        @Override
        protected String doInBackground(InputStream... params) {
            try {
                parseELF(callback, params[0]);
            } catch(Exception e) {
                e.printStackTrace();
                return "failed";
            }
            return getString(R.string.success);
        }

        
        @Override
        protected void onPostExecute(String result) {
            
            dlg.dismiss();
            
            if(!result.equals(getString(R.string.success))) {
                
                showMessage(ElfActivity.this, result).show();
                return;
            }
            
            Collections.sort(Types);
            
            AsyncTask<String, Void, Void> getTask = new GetTask();
            getTask.execute(textCategory.getText().toString());
        }

        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dlg = new ProgressDialog(ElfActivity.this);
            dlg.setCancelable(false);
            dlg.setTitle(R.string.parsing);
            dlg.show();
            textCategory.setText("dynstr");
            
            if(Types == null) {
                
                Types = new ArrayList<String>();
            }
            
            callback = new ResourceCallBack() {
                @Override
                public void back(ResourceHelper helper) {
                    if(RESOURCES == null)
                        RESOURCES = new LinkedHashMap<String, ResourceHelper>();
                    RESOURCES.put(helper.VALUE, helper);
                    
                    if(!Types.contains(helper.TYPE)) {
                        
                        Types.add(helper.TYPE);
                    }
                }
            };
        }

        
        @Override
        protected void onProgressUpdate(Integer... values) {
            dlg.setMessage(String.valueOf(values[0]));
        }

    }

    /**
     * @author com.perezhai 
     */
    class SaveFileTask extends AsyncTask<String, String, String> {
        
        private ProgressDialog dlg;

        
        @Override
        protected String doInBackground(String... params) {
            try {
                writeELFString((String) params[0]);
            } catch(IOException e) {
                e.printStackTrace();
                return e.toString();
            }
            return getString(R.string.success);
        }

        
        @Override
        protected void onPostExecute(String result) {
            
            dlg.dismiss();
            
            if(!result.equals(getString(R.string.success))) {
                
                showMessage(ElfActivity.this, result).show();
                return;
            }
            finish();
        }

        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            
            dlg = new ProgressDialog(ElfActivity.this);
            
            dlg.setTitle(R.string.saving);
            
            dlg.setCancelable(false);
            
            dlg.show();
        }

    }

    
    public class stringListAdapter extends BaseAdapter {

        
        private Context mContext;

        
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

        @SuppressLint({ "ViewHolder", "InflateParams" })
        @Override
        public View getView(final int position, View view, ViewGroup arg2) {
            
            TextWatcher textWatcher = new TextWatcher() {
                
                @Override
                public void afterTextChanged(Editable s) {
                    
                    txtTranslated.set(position, s.toString());
                    isChanged = true;
                }
                
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            };
            
            view = LayoutInflater.from(mContext).inflate(R.layout.res_string_item, null);
            
            final TextView txtOriginalView = (TextView) view.findViewById(R.id.txtOriginal);
            
            EditText txtTranslatedView = (EditText) view.findViewById(R.id.txtTranslated);
            
            txtOriginalView.setText(txtOriginal.get(position));
            
            txtTranslatedView.setText(txtTranslated.get(position));
            
            txtTranslatedView.addTextChangedListener(textWatcher);
            View.OnLongClickListener longclick_listener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClipboardManager cm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    
                    cm.setText(txtOriginalView.getText());
                    Toast.makeText(ElfActivity.this, "Succeeded copying to clipboard!", Toast.LENGTH_LONG).show();
                    return true;
                }
            };
            txtOriginalView.setOnLongClickListener(longclick_listener);
            return view;
        }
    }

    
    public static List<String> Types;

    /**
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static byte[] InputStream2ByteArray(InputStream is) throws IOException {
        int count;
        byte[] buffer = new byte[2048];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while((count = is.read(buffer)) != -1)
            bos.write(buffer, 0, count);
        bos.close();
        return bos.toByteArray();
    }

    
    public static AlertDialog.Builder showMessage(Context activity, String message) {
        return new AlertDialog.Builder(activity).setMessage(message).setNegativeButton(R.string.ok, null)
               .setCancelable(false).setTitle(R.string.error);
    }

    private String fileSrc = "";

    private Elf elfParser;

    
    public List<String> txtOriginal = new ArrayList<String>();

    
    public List<String> txtTranslated = new ArrayList<String>();

    
    public ListView stringListView;

    
    public stringListAdapter mAdapter;

    
    private Map<String, ResourceHelper> RESOURCES;

    
    private TextView textCategory;

    
    public boolean isChanged;

    /**
     * 
     *
     * @author com.perezhai
     */
    private TextWatcher textWatcher = new TextWatcher() {
        
        @Override
        public void afterTextChanged(Editable s) {
            
            AsyncTask<String, Void, Void> task = new GetTask();
            
            task.execute(textCategory.getText().toString());
        }
        
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // TODO Auto-generated method stub
        }
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

    
    private OnClickListener MyOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View arg0) {
            // TODO Auto-generated method stub
            switch(arg0.getId()) {
            
            case R.id.textCategory:
                
                new AlertDialog.Builder(ElfActivity.this).setTitle("")
                .setItems(Types.toArray(new String[Types.size()]), new DialogInterface.OnClickListener() {
                    
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        textCategory.setText(Types.get(arg1));
                    }
                }).create().show();
                break;
            }
        }
    };

    private boolean checkChanged() {
        for(String str : txtTranslated) {
            if(!str.equals(""))
                return true;
        }
        return false;
    }

    /**  **/
    private void initList() {
        for(int i = 0; i < txtOriginal.size(); i++) {
            
            txtTranslated.add("");
        }
    }

    /**  */
    @Override
    public void onBackPressed() {
        if(isChanged || checkChanged()) {
            
            showSaveDialog();
        } else {
            finish();
            //	PerezReverseKillerMain.mAdapter.notifyDataSetInvalidated();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.string_list);
        
        stringListView = (ListView) findViewById(R.id.list_res_string);
        
        textCategory = (TextView) findViewById(R.id.textCategory);
        
        textCategory.setOnClickListener(MyOnClickListener);
        
        textCategory.addTextChangedListener(textWatcher);
        
        mAdapter = new stringListAdapter(this);
        
        stringListView.setAdapter(mAdapter);
        fileSrc = getIntent().getStringExtra("FILE_NAME");
        if(!fileSrc.isEmpty()) {
            try {
                this.open(new FileInputStream(fileSrc));
            } catch(FileNotFoundException e) {
                e.printStackTrace();
                finish();
            }
        } else finish();
    }


    public void SaveF() {
        File file = new File(fileSrc);
        File bak = new File(fileSrc + ".bak");
        file.renameTo(bak);
        file.delete();
        SaveFileTask saveTask = new SaveFileTask();
        saveTask.execute(fileSrc);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_elfedit, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()) {
        case R.id.e_save:
            SaveF();
            break;
        case R.id.e_exit:
            finish();
            //	PerezReverseKillerMain.mAdapter.notifyDataSetInvalidated();
            break;
        }
        return true;
    }


    private void open(InputStream resInputStream) {
        
        AsyncTask<InputStream, Integer, String> task = new ParseTask();
        try {
            
            task.execute(resInputStream);
        } catch(OutOfMemoryError e) {
            showMessage(this, getString(R.string.out_of_memory)).show();
        }
        
        AsyncTask<String, Void, Void> getTask = new GetTask();
        
        getTask.execute(textCategory.getText().toString());
    }

    /**
     * ELF
     *
     * @param result
     *            
     * @param is
     *            
     **/
    public void parseELF(ResourceCallBack callBack, InputStream is)
    throws UnknownFormatConversionException, IOException {
        elfParser = new Elf(new ByteArrayInputStream(InputStream2ByteArray(is)), callBack);
    }

    /**  **/
    private void showSaveDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.notice).setMessage(R.string.ensure_save)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File file = new File(fileSrc);
                File bak = new File(fileSrc + ".bak");
                file.renameTo(bak);
                file.delete();
                SaveFileTask saveTask = new SaveFileTask();
                saveTask.execute(fileSrc);
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setResult(Activity.RESULT_CANCELED, getIntent());
                finish();
            }
        }).create().show();
    }

    
    @SuppressLint("DefaultLocale")
    public void writeELFString(String output) throws IOException {
        if(textCategory.getText().toString().equals("rodata"))
            elfParser.sortStrData(txtOriginal, txtTranslated, elfParser.ro_items);
        else {
            elfParser.sortStrData(txtOriginal, txtTranslated, elfParser.dy_items);
        }
        OutputStream fos = new FileOutputStream(output);
        elfParser.writeELF(fos);
        fos.close();
    }
}
