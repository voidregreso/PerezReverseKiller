package com.perez.netdiagnosis.Activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;

import java.nio.charset.Charset;

import com.perez.revkiller.R;
import com.perez.catchexception.CrashApp;
import com.perez.revkiller.databinding.ActivityJsonBinding;

public class JsonPreviewActivity extends AppCompatActivity {
    private ActivityJsonBinding binding;

    private Handler mHandler = new Handler();
    private String content;
    private int selectedEncode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJsonBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        setupActionBar();

        try {
            int pos = getIntent().getIntExtra("pos",-1);
            if(pos > -1){
                HarLog harLog = ((CrashApp) getApplication()).proxy.getHar().getLog();
                HarEntry harEntry = harLog.getEntries().get(pos);
                content = harEntry.getResponse().getContent().getText();
                initViewDelay(content);
            }else{
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        setTitle("Content details");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void initViewDelay(final String content) {
        getWindow().getDecorView().post(
                new Runnable() {
                    @Override
                    public void run() {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                formatContent(content);
                            }
                        });
                    }
                }
        );

    }

    public void formatContent(String content) {
        try {
            binding.tvDetailLayout.setText(jsonFormatter(content));
        } catch (Exception e) {
            binding.tvDetailLayout.setText(content);
        }
    }

    public String jsonFormatter(String uglyJSONString) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(uglyJSONString);
        return gson.toJson(je);
    }

    private String[] encodeItem = new String[]{"UTF-8", "ISO-8859-1", "GBK"};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.json_menu, menu);
        MenuItem encodeButton = menu.findItem(R.id.encode);
        encodeButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                DialogInterface.OnClickListener listener = new ButtonOnClick();
                AlertDialog.Builder builder = new AlertDialog.Builder(JsonPreviewActivity.this);
                builder.setNegativeButton(R.string.cancel,null);
                builder.setPositiveButton(R.string.ok, listener);
                builder.setSingleChoiceItems(encodeItem,selectedEncode,listener);
                builder.create().show();
                return true;
            }
        });


        return super.onCreateOptionsMenu(menu);
    }

    private class ButtonOnClick implements DialogInterface.OnClickListener {

        private int index = -1; 

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which >= 0) {
                index = which;
            } else {
                
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    selectedEncode = index;
                    changeEncode(index);
                }
            }
        }
    }

    public void changeEncode(int pos){
        switch (pos){
            case 0:
                initViewDelay(content);
                break;
            case 1:
                initViewDelay(new String(content.getBytes(Charset.forName("ISO-8859-1")), Charset.forName("UTF-8")));
                break;
            case 2:
                initViewDelay(new String(content.getBytes(Charset.forName("GBK")), Charset.forName("UTF-8")));
                break;
            default:
                initViewDelay(content);
                break;
        }

    }
}
