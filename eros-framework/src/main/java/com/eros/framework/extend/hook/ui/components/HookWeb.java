package com.eros.framework.extend.hook.ui.components;


import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.eros.framework.BMWXEnvironment;
import com.eros.framework.manager.impl.FileManager;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.annotation.Component;
import com.taobao.weex.adapter.URIAdapter;
import com.taobao.weex.common.Constants;
import com.taobao.weex.dom.WXDomObject;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXComponentProp;
import com.taobao.weex.ui.component.WXVContainer;
import com.taobao.weex.ui.view.IWebView;
import com.taobao.weex.ui.view.WXWebView;
import com.taobao.weex.utils.WXUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by liuyuanxiao on 2018/7/3.
 */
@Component(lazyload = false)
public class HookWeb extends WXComponent {
    private final String LOCAL_SCHEME = "bmlocal";
    public static final String GO_BACK = "goBack";
    public static final String GO_FORWARD = "goForward";
    public static final String RELOAD = "reload";
    protected IWebView mWebView;

    @Deprecated
    public HookWeb(WXSDKInstance instance, WXDomObject dom, WXVContainer parent, String instanceId, boolean isLazy) {
        this(instance, dom, parent, isLazy);
    }

    public HookWeb(WXSDKInstance instance, WXDomObject dom, WXVContainer parent, boolean isLazy) {
        super(instance, dom, parent, isLazy);
        createWebView();
    }

    protected void createWebView() {
        mWebView = new BMWXWebView(getContext());
    }

    @Override
    protected View initComponentHostView(@NonNull Context context) {
        mWebView.setOnErrorListener(new IWebView.OnErrorListener() {
            @Override
            public void onError(String type, Object message) {
                fireEvent(type, message);
            }
        });
        mWebView.setOnPageListener(new IWebView.OnPageListener() {
            @Override
            public void onReceivedTitle(String title) {
                if (getDomObject().getEvents().contains(Constants.Event.RECEIVEDTITLE)) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("title", title);
                    fireEvent(Constants.Event.RECEIVEDTITLE, params);
                }
            }

            @Override
            public void onPageStart(String url) {
                if (getDomObject().getEvents().contains(Constants.Event.PAGESTART)) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("url", url);
                    fireEvent(Constants.Event.PAGESTART, params);
                }
            }

            @Override
            public void onPageFinish(String url, boolean canGoBack, boolean canGoForward) {
                if (getDomObject().getEvents().contains(Constants.Event.PAGEFINISH)) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("url", url);
                    params.put("canGoBack", canGoBack);
                    params.put("canGoForward", canGoForward);
                    fireEvent(Constants.Event.PAGEFINISH, params);
                }
            }
        });
        return mWebView.getView();
    }

    @Override
    public void destroy() {
        super.destroy();
        getWebView().destroy();
    }

    @Override
    protected boolean setProperty(String key, Object param) {
        switch (key) {
            case Constants.Name.SHOW_LOADING:
                Boolean result = WXUtils.getBoolean(param, null);
                if (result != null)
                    setShowLoading(result);
                return true;
            case Constants.Name.SRC:
                String src = WXUtils.getString(param, null);
                if (src != null)
                    setUrl(src);
                return true;
        }
        return super.setProperty(key, param);
    }

    @WXComponentProp(name = Constants.Name.SHOW_LOADING)
    public void setShowLoading(boolean showLoading) {
        getWebView().setShowLoading(showLoading);
    }

    private String info;

    @WXComponentProp(name = "mapData")
    public void setMapData(String info) {

        this.info = info;

        fireEvent("finish");
        Log.e("info", info);
    }

    @WXComponentProp(name = Constants.Name.SRC)
    public void setUrl(String url) {
        Log.e("setUrl", "setUrl");
        if (TextUtils.isEmpty(url) || getHostView() == null) {
            return;
        }
        if (!TextUtils.isEmpty(url)) {
            Uri ul = getInstance().rewriteUri(Uri.parse(url), URIAdapter.WEB);
            if (LOCAL_SCHEME.equalsIgnoreCase(ul.getScheme())) {
                String mUrl = "file://" + localPath(ul);
                loadUrl(mUrl);
            } else {
                BMWXWebView webView = (BMWXWebView) mWebView;
                WebSettings settings = webView.getWebView().getSettings();

                settings.setJavaScriptEnabled(true);
                settings.setAllowFileAccess(true);
                settings.setDomStorageEnabled(true);
                //   webView.getWebView().setWebViewClient(new WebViewClient());
                webView.getWebView().setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {

                        super.onPageStarted(view, url, favicon);


                    }

                    @Override
                    public void onPageCommitVisible(WebView view, String url) {

                        super.onPageCommitVisible(view, url);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {

                        super.onPageFinished(view, url);
                        if (!TextUtils.isEmpty(info)) {
                            loadUrl("javascript:setOptions(" + info + ")");
                        }

                    }
                });
                loadUrl(ul.toString());

            }
        }
    }

    private static final String INSIDE_URL = "file:///android_asset/direction.html";


    private String getUrl(String url) {
        return TextUtils.isEmpty(url) ? "" : getAssetsPath(url);
    }

    private String getAssetsPath(String path) {
        Uri uri = Uri.parse(path);
        if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
            return path;
        }
        if ("bmlocal".equalsIgnoreCase(uri.getScheme())) {
            return BMWXEnvironment.loadBmLocal(getContext(), uri);
        }
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return path;
        }
        return INSIDE_URL;
    }

    private String localPath(Uri uri) {
        String path = uri.getHost() + File.separator +
                uri.getPath() + "?" + uri.getQuery();
        return FileManager.getPathBundleDir(getContext(), "bundle/" + path)
                .getAbsolutePath();
    }

    public void setAction(String action) {
        if (!TextUtils.isEmpty(action)) {
            if (action.equals(GO_BACK)) {
                goBack();
            } else if (action.equals(GO_FORWARD)) {
                goForward();
            } else if (action.equals(RELOAD)) {
                reload();
            }
        }
    }

    private void fireEvent(String type, Object message) {
        if (getDomObject().getEvents().contains(Constants.Event.ERROR)) {
            Map<String, Object> params = new HashMap<>();
            params.put("type", type);
            params.put("errorMsg", message);
            fireEvent(Constants.Event.ERROR, params);
        }
    }

    private void loadUrl(String url) {
        getWebView().loadUrl(url);
    }

    private void reload() {
        getWebView().reload();
    }

    private void goForward() {
        getWebView().goForward();
    }

    private void goBack() {
        getWebView().goBack();
    }

    private IWebView getWebView() {
        return mWebView;
    }

}
