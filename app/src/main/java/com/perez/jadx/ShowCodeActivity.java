package com.perez.jadx;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.webkit.WebSettings;

import com.perez.revkiller.R;

import java.util.ArrayList;

import com.perez.code.CodeView;
import com.perez.code.CodeViewTheme;

public class ShowCodeActivity extends AppCompatActivity {
    CodeView codeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_code);
        ArrayList<String> str = getIntent().getStringArrayListExtra("fileName");
        setTitle(str.get(0));
        codeView = findViewById(R.id.codeview);
        codeView.setTheme(CodeViewTheme.IDEA);
        WebSettings webSettings = codeView.getSettings();
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUseWideViewPort(true);
        codeView.showCode(str.get(1));
    }


}
