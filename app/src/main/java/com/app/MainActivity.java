//I will run `./gradlew assembleDebug` to verify that the project builds successfully before we make our changes.
package com.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    WebView webView;
    private AutoCompleteTextView urlInput;
    private ArrayAdapter<String> adapter;
    private List<String> historyList;

    private static final String PREFS_NAME = "UrlHistoryPrefs";
    private static final String KEY_HISTORY = "history_list";
    private static final String KEY_LAST_URL = "last_url";
    private static final int MAX_HISTORY = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
    View.SYSTEM_UI_FLAG_FULLSCREEN
    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
);


        if (android.os.Build.VERSION.SDK_INT >= 28) {
    getWindow().getAttributes().layoutInDisplayCutoutMode =
        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        // Fullscreen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        webView = new WebView(this);
        LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        );
        webView.setLayoutParams(webViewParams);
        mainLayout.addView(webView);

        setContentView(mainLayout);

        WebSettings s = webView.getSettings();

        // Core performance settings
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        //s.setAppCacheEnabled(true);
        //s.setAppCachePath(getCacheDir().getAbsolutePath());
        s.setAllowContentAccess(true);
        s.setAllowFileAccess(true);
        s.setBlockNetworkImage(false);
        s.setLoadsImagesAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            s.setOffscreenPreRaster(true);
        }
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setFitsSystemWindows(false);
        webView.setPadding(0,0,0,0);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (url != null) {
                    urlInput.setText(url, false);
                    urlInput.setSelection(url.length());
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && !url.trim().isEmpty()) {
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().putString(KEY_LAST_URL, url).apply();
                }
            }
        });

        historyList = loadHistory();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, historyList);

        // Prepare an input field (not added to the main layout). We'll show it inside a startup dialog.
        urlInput = new AutoCompleteTextView(this);
        urlInput.setHint("Enter URL");
        urlInput.setSingleLine(true);
        urlInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setThreshold(1);
        urlInput.setAdapter(adapter);

        // Load last URL or default
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastUrl = prefs.getString(KEY_LAST_URL, "https://www.soundcraft.com/ui24-software-demo/mixer.html");

        showUrlDialog(lastUrl);
    }

    private List<String> loadHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String historyStr = prefs.getString(KEY_HISTORY, "");
        List<String> history = new ArrayList<>();
        if (!historyStr.isEmpty()) {
            String[] urls = historyStr.split("\n");
            for (String url : urls) {
                String trimmed = url.trim();
                if (!trimmed.isEmpty()) {
                    history.add(trimmed);
                }
            }
        }
        return history;
    }

    private void saveUrlToHistory(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        url = url.trim();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LAST_URL, url);

        List<String> history = loadHistory();
        history.remove(url);
        history.add(0, url);
        if (history.size() > MAX_HISTORY) {
            history = history.subList(0, MAX_HISTORY);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            sb.append(history.get(i));
            if (i < history.size() - 1) {
                sb.append("\n");
            }
        }
        editor.putString(KEY_HISTORY, sb.toString());
        editor.apply();

        updateAdapter(history);
    }

    private void updateAdapter(List<String> newHistory) {
        if (historyList != null && adapter != null) {
            historyList.clear();
            historyList.addAll(newHistory);
            adapter.notifyDataSetChanged();
        }
    }

    private void loadAndSaveUrl() {
        String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) {
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        webView.loadUrl(url);
        saveUrlToHistory(url);

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
        }
        urlInput.clearFocus();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        }
    }

    private void showUrlDialog(final String lastUrl) {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Open URL");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        container.addView(urlInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        if (lastUrl != null) {
            urlInput.setText(lastUrl, false);
            urlInput.setSelection(lastUrl.length());
        }

        builder.setView(container);
        builder.setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                loadAndSaveUrl();
            }
        });
        builder.setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                if (lastUrl != null && !lastUrl.trim().isEmpty()) {
                    webView.loadUrl(lastUrl);
                }
                dialog.dismiss();
            }
        });

        final android.app.AlertDialog dialog = builder.create();

        urlInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    loadAndSaveUrl();
                    dialog.dismiss();
                    return true;
                }
                return false;
            }
        });

        urlInput.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                loadAndSaveUrl();
                dialog.dismiss();
            }
        });

        urlInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    urlInput.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing() && urlInput.hasFocus()) {
                                urlInput.showDropDown();
                            }
                        }
                    });
                }
            }
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        urlInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(urlInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }
}
