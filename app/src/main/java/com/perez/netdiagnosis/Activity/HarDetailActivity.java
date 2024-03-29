package com.perez.netdiagnosis.Activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import net.lightbody.bmp.core.har.HarCookie;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.core.har.HarPostDataParam;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;

import com.perez.revkiller.R;
import com.perez.catchexception.CrashApp;
import com.perez.revkiller.databinding.ActivityScrollingBinding;

public class HarDetailActivity extends AppCompatActivity {
    private ActivityScrollingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScrollingBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        setupActionBar();

        try {
            initHarLog(getIntent().getIntExtra("pos", -1));
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    public void initHarLog(int pos) {
        HarLog harLog = ((CrashApp) getApplication()).proxy.getHar().getLog();
        HarEntry harEntry = harLog.getEntries().get(pos);

        HarRequest harRequest = harEntry.getRequest();
        HarResponse harResponse = harEntry.getResponse();

        addItem("Overview");
        addItem("URL", harRequest.getUrl());

        addItem("Method", harRequest.getMethod());
        addItem("Code", harResponse.getStatus() + "");
        addItem("TotalTime", harEntry.getTime() + "ms");
        addItem("Size", harResponse.getBodySize() + "Bytes");

        if (harRequest.getQueryString().size() > 0) {
            addItem("Request Query");
            for (HarNameValuePair pair : harRequest.getQueryString()) {
                addItem(pair.getName(), pair.getDecodeValue());
            }
        }

        addItem("Request Header");
        for (HarNameValuePair pair : harRequest.getHeaders()) {
            
            if (!pair.getName().equals("Cookie")) {
                addItem(pair.getName(), pair.getDecodeValue());
            }
        }

        if (harRequest.getCookies().size() > 0) {
            addItem("Request Cookies");
            for (HarCookie cookie : harRequest.getCookies()) {
                addItem(cookie.getName(), cookie.getDecodeValue());
            }
        }

        if (harRequest.getPostData() != null) {
            if(harRequest.getPostData().getText()!= null
                    && harRequest.getPostData().getText().length()>0) {
                addItem("Request Content");
                addItem("PostData", harRequest.getPostData().getText());
            }

            if(harRequest.getPostData().getParams()!= null
                    &&  harRequest.getPostData().getParams().size()>0){
                addItem("Request PostData");

                for (HarPostDataParam pair : harRequest.getPostData().getParams()) {
                    addItem(pair.getName(), pair.getValue());
                }
            }
        }

        addItem("Response Header");
        for (HarNameValuePair pair : harResponse.getHeaders()) {
            if (!pair.getName().equals("Cookie")) {
                addItem(pair.getName(), pair.getDecodeValue());
            }
        }

        if (harResponse.getCookies().size() > 0) {
            addItem("Response Cookies");
            for (HarCookie cookie : harResponse.getCookies()) {
                addItem(cookie.getName(), cookie.getDecodeValue());
            }
        }

        if ((harResponse.getRedirectURL() != null && harResponse.getRedirectURL().length() > 0) ||
                (harResponse.getContent().getText() != null && harResponse.getContent().getText().length() > 0)) {
            addItem("Response Content");
        }
        if (harResponse.getRedirectURL() != null && harResponse.getRedirectURL().length() > 0) {
            addItem("RedirectURL", harResponse.getRedirectURL());
        }
        if (harResponse.getContent().getText() != null && harResponse.getContent().getText().length() > 0) {
            addContentItem("Content", harResponse.getContent().getText(), pos);
        }

    }

    public void addContentItem(String title, final String value, final int pos) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_detail, null);

        TextView textView = (TextView) view.findViewById(R.id.tv_title);
        textView.setText(title);

        TextView valueTextView = (TextView) view.findViewById(R.id.tv_value);
        if (TextUtils.isEmpty(value)) {
            valueTextView.setText("");
        } else {
            valueTextView.setText(value.substring(0, value.length() > 50 ? 50 : value.length()));
        }

        if (title.equals("Content")) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (value != null && value.length() > 10) {
                        Intent intent = new Intent(HarDetailActivity.this, JsonPreviewActivity.class);
                        intent.putExtra("pos", pos);
                        HarDetailActivity.this.startActivity(intent);
                    }
                }
            });
        }
        binding.llDetailLayout.addView(view);
    }

    public void addItem(String title, final String value) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_detail, null);

        TextView textView = (TextView) view.findViewById(R.id.tv_title);
        textView.setText(title);

        TextView valueTextView = (TextView) view.findViewById(R.id.tv_value);
        if (TextUtils.isEmpty(value)) {
            valueTextView.setText("");
        } else {
            valueTextView.setText(value.substring(0, value.length() > 50 ? 50 : value.length()));
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (value != null && value.length() > 10) {
                    View textEntryView = LayoutInflater.from(HarDetailActivity.this).inflate(R.layout.alert_textview, null);
                    TextView edtInput = (TextView) textEntryView.findViewById(R.id.tv_content);
                    edtInput.setText(value);

                    AlertDialog.Builder builder = new AlertDialog.Builder(HarDetailActivity.this);
                    builder.setCancelable(true);
                    builder.setView(textEntryView);
                    builder.setPositiveButton(R.string.ok, null);
                    builder.show();
                }
            }
        });


        binding.llDetailLayout.addView(view);
    }

    public void addItem(String cateName) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_cate, null);
        TextView textView = (TextView) view.findViewById(R.id.tv_catetitle);
        textView.setText(cateName);
        binding.llDetailLayout.addView(view);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        setTitle("Data details");
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
}
