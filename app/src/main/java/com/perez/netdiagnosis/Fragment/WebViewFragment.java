package com.perez.netdiagnosis.Fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.perez.netdiagnosis.Activity.NDGAct;
import com.perez.revkiller.R;

import com.perez.catchexception.CrashApp;

import com.perez.netdiagnosis.Utils.DeviceUtils;
import com.perez.netdiagnosis.Utils.ProxyUtils;
import com.perez.netdiagnosis.Utils.SharedPreferenceUtils;
import com.perez.revkiller.databinding.FragmentWebviewBinding;

public class WebViewFragment extends BaseFragment {
    private FragmentWebviewBinding binding;
    
    Receiver receiver;

    public Boolean isSetProxy = false;

    public String baseUserAgentString = "Mozilla/5.0 (Linux; Android 5.0.2) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/37.0.0.0";

    public String userAgentString = baseUserAgentString;

    private final static WebViewFragment webViewFragment = new WebViewFragment();

    public static WebViewFragment getInstance() {
        return webViewFragment;
    }

    @Override
    public boolean onBackPressed() {
        if (binding.flWebview.canGoBack()) {
            binding.flWebview.goBack();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentWebviewBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        isSetProxy = false;

        binding.etUrl.setText(NDGAct.HOME_URL);

        WebSettings webSettings = binding.flWebview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDatabaseEnabled(true);
        String dir = getActivity().getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        webSettings.setDatabasePath(dir);
        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationEnabled(true);

        baseUserAgentString = webSettings.getUserAgentString()+" jdhttpmonitor/" + DeviceUtils.getVersion(getContext());
        webSettings.setUserAgentString(userAgentString);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        binding.flWebview.setDownloadListener(new MyWebViewDownLoadListener());
        binding.flWebview.setWebViewClient(new WebViewClient() {

//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//                if(request.getUrl().toString().startsWith("jdhttpmonitor://webview")) {
//                    Intent intent = new Intent("android.intent.action.VIEW");
//                    intent.setData(Uri.parse(request.getUrl().toString()));
//                    startActivity(intent);
//                    return true;
//                }
//                return false;
//            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url.startsWith("jdhttpmonitor://webview")) {
                    Intent intent = new Intent("android.intent.action.VIEW");
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                binding.etUrl.setText(url);
            }
        });

        binding.flWebview.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
//                super.onConsoleMessage(message, lineNumber, sourceID);
//                ((NDGAct)getActivity()).consoleLog.append(message).append("\n").append("\n");
//            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                ((NDGAct)getActivity()).consoleLog.append(consoleMessage.message()).append("\n").append("\n");
                return super.onConsoleMessage(consoleMessage);
            }

            public void onProgressChanged(WebView view, int progress) {
                binding.pbProgress.setProgress(progress);
                if (progress == 100) {
                    binding.pbProgress.setVisibility(View.GONE);
                    binding.swipeContainer.setRefreshing(false);
                } else {
                    binding.pbProgress.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.btJump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.etUrl.getText().length() > 0) {
                    loadUrl(binding.etUrl.getText() + "");
                }
            }
        });

        binding.etUrl.setImeOptions(EditorInfo.IME_ACTION_GO);
        binding.etUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                
                if (actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode()
                        && KeyEvent.ACTION_DOWN == event.getAction())) {
                    if (binding.etUrl.getText().length() > 0) {
                        loadUrl(v.getText() + "");
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                    }
                    
                }
                return false;
            }
        });

        binding.swipeContainer.setColorSchemeResources(R.color.colorAccentDark,R.color.colorAccent);

        binding.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                
                binding.flWebview.loadUrl(binding.flWebview.getUrl());
            }
        });

        initProxyWebView();

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            ((NDGAct) getActivity()).getBinding().navView.getMenu().getItem(0).setChecked(true);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        receiver = new Receiver();
        getActivity().registerReceiver(receiver, new IntentFilter("proxyfinished"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(receiver);
    }

    public void initProxyWebView() {
        if (getActivity() != null && CrashApp.isInitProxy && !isSetProxy) {
            binding.flWebview.post(new Runnable() {
                @Override
                public void run() {
                    if(ProxyUtils.setProxy(binding.flWebview, "127.0.0.1", CrashApp.proxyPort)){
                        Log.e("~~~~", "initProxyWebView()");
                        binding.flWebview.loadUrl(binding.etUrl.getText() + "");
                        isSetProxy = true;
                    }else{
                        Toast.makeText(binding.flWebview.getContext(),"Set proxy fail!",Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    public class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            initProxyWebView();
            Log.i("~~~~", "Receiver initProxyWebView");
        }
    }

    public void loadUrl(String url) {
        if (binding.flWebview != null) {
            if (!isSetProxy) {
                ProxyUtils.setProxy(binding.flWebview, "127.0.0.1", CrashApp.proxyPort);
                Log.e("~~~~", "initProxyWebView()");
                isSetProxy = true;
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            binding.etUrl.setText(url);
            binding.flWebview.loadUrl(url);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setUserAgent();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    public void setUserAgent(){
        String originUA = userAgentString;

        switch (SharedPreferenceUtils.getString(getContext(),"select_ua", "0")) {
            case "0":
                userAgentString = baseUserAgentString;
                break;
            case "1":
                userAgentString = baseUserAgentString + " MQQBrowser/6.2 TBS/036524 MicroMessenger/6.3.18.800 NetType/WIFI Language/zh_CN";
                break;
            case "2":
                userAgentString = baseUserAgentString + " MQQBrowser/6.2 TBS/036524 V1_AND_SQ_6.0.0_300_YYB_D QQ/6.0.0.2605 NetType/WIFI WebP/0.3.0 Pixel/1440";
                break;
        }
        WebSettings webSettings = binding.flWebview.getSettings();
        webSettings.setUserAgentString(userAgentString);

        if(!originUA.equals(userAgentString) && binding.flWebview!=null){
            reload();
        }
    }

    public void reload(){
        if(binding.flWebview!=null && binding.flWebview.getUrl()!=null) {
            binding.flWebview.reload();
        }
    }

    private class MyWebViewDownLoadListener implements DownloadListener {
        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,
                                    long contentLength) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }
}
