package com.stanfy.utils;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.View;
import android.webkit.WebView;

import com.stanfy.utils.notifications.BaseNotificationBuilder;
import com.stanfy.utils.notifications.NotificationBuilder;

/**
 * Implementation for old versions.
 * @author Roman Mazur - Stanfy (http://www.stanfy.com)
 */
public class LowestSDKDependentUtils implements SDKDependentUtils {

  @Override
  public File getExternalCacheDir(final Context context) {
    return new File(Environment.getExternalStorageDirectory(), "/Android/data/" + context.getPackageName() + "/cache");
  }

  @Override
  public File getMusicDirectory() {
    return new File(Environment.getExternalStorageDirectory(), "Music");
  }

  @Override
  public void setOverscrollNever(final View view) { /* not implemented */ }

  @Override
  public void enableStrictMode() { /* not implemented */ }

  @Override
  public void applySharedPreferences(final Editor editor) {
    editor.commit();
  }

  @Override
  public void webViewOnPause(final WebView webView) { /* not implemented */ }

  @Override
  public void webViewOnResume(final WebView webView) { /* not implemented */ }

  @Override
  public <P> void executeAsyncTaskParallel(final AsyncTask<P, ?, ?> task, final P... params) {
    // DONUT..GINGERBREAD - its parallel
    task.execute(params);
  }

  @Override
  public NotificationBuilder createNotificationBuilder(final Context context) {
    return new BaseNotificationBuilder(context);
  }

}