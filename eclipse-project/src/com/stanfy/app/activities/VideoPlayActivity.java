package com.stanfy.app.activities;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.stanfy.app.Application;
import com.stanfy.app.fragments.VideoPlayFragment;

/**
 * Activity to play video. It does not contain action bar.
 * @author Roman Mazur (Stanfy - http://www.stanfy.com)
 */
public class VideoPlayActivity extends OneFragmentActivity<Application> {

  /** Fragment. */
  private VideoPlayFragment fragment;

  @Override
  protected int getFragmentContainerId() { return android.R.id.content; }
  @Override
  protected int getLayoutId() { return 0; }

  @Override
  protected Fragment createFragment(final Bundle savedInstanceState) {
    final Uri uri = getIntent().getData();
    fragment = VideoPlayFragment.create(uri);
    return fragment;
  }

  @Override
  public void onConfigurationChanged(final Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (fragment != null) { fragment.invalidateView(); }
  }

}
