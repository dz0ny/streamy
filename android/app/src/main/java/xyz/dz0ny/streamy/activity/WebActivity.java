/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package xyz.dz0ny.streamy.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import xyz.dz0ny.streamy.service.Server;


/*
 * WebActivity class that loads {@link MainFragment}.
 */

public class WebActivity extends Activity {
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebView = new WebView(this);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String loc = request.getUrl().toString();
                if (loc.endsWith(".mkv") || loc.endsWith(".mp4") || loc.endsWith(".avi")) {
                    Intent vlcIntent = new Intent(Intent.ACTION_VIEW);
                    vlcIntent.setPackage("org.videolan.vlc");
                    vlcIntent.setComponent(new ComponentName("org.videolan.vlc", "org.videolan.vlc.gui.video.VideoPlayerActivity"));
                    vlcIntent.setDataAndTypeAndNormalize(request.getUrl(), "video/*");
                    getApplicationContext().startActivity(vlcIntent);
                    return true;
                }
                return false;
            }

        });
        setContentView(mWebView);
    }

    // We start the server on onResume
    @Override
    protected void onResume() {
        super.onResume();
        if (!isMyServiceRunning(Server.class)) {
            Intent intent = new Intent(this, Server.class);
            this.startService(intent);
        }
        try {
            mWebView.loadUrl("http://127.0.0.1:9092/");
        } catch (Exception e) {
            Toast.makeText(this, "Error:" + e.toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            this.finish();
        }
    }

    // Send a graceful shut down signal to the server. onPause is guaranteed
    // to be called by Android while onStop or onDestroy may not be called.
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                mWebView.reload();
                return true;
            }
        }
        return false;
    }

}

