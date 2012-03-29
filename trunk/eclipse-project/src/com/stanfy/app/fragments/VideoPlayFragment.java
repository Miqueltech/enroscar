package com.stanfy.app.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;

import com.stanfy.DebugFlags;
import com.stanfy.app.Application;
import com.stanfy.app.BaseDialogFragment;
import com.stanfy.app.BaseFragment;
import com.stanfy.views.R;

/**
 * Fragment that displays the video player.
 * @author Roman Mazur (Stanfy - http://www.stanfy.com)
 */
public class VideoPlayFragment extends BaseFragment<Application> implements OnPreparedListener, OnErrorListener, OnCompletionListener {

  /** Logging tag. */
  private static final String TAG = "VideoFragment";

  /** Debug flags. */
  private static final boolean DEBUG = DebugFlags.DEBUG_GUI;

  /** Extra keys. */
  public static final String EXTRA_SOURCE_URI = "src_uri",
                             EXTRA_FINISH_ON_COMPLETE = "fin_on_complete";

  /** Video view. */
  private VideoView videoView;
  /** Progress indicator. */
  private View progress;

  /** Media controller. */
  private MediaController controller;

  /** Handler. */
  private Handler handler = new Handler();

  /** Wake lock. */
  private WakeLock wakeLock;

  /** Show controller worker. */
  private final Runnable showController = new Runnable() {
    @Override
    public void run() {
      if (DEBUG) { Log.d(TAG, "Show video controller"); }
      if (controller != null) {
        controller.show();
      }
    }
  };

  /** 'Completed' flag. */
  private boolean completed = false;

  /** Finish on complete flag. */
  private boolean finishOnComplete = true;

  /**
   * @param source source URI
   * @return fragment instance
   */
  public static VideoPlayFragment create(final Uri source) {
    return create(source, true);
  }
  /**
   * @param source source URI
   * @param finishOnComplete finish on complete flag
   * @return fragment instance
   */
  public static VideoPlayFragment create(final Uri source, final boolean finishOnComplete) {
    final VideoPlayFragment result = new VideoPlayFragment();
    final Bundle args = new Bundle();
    args.putParcelable(EXTRA_SOURCE_URI, source);
    args.putBoolean(EXTRA_FINISH_ON_COMPLETE, finishOnComplete);
    result.setArguments(args);
    return result;
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final PowerManager powerManager = (PowerManager)getOwnerActivity().getSystemService(Context.POWER_SERVICE);
    wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
    finishOnComplete = getArguments().getBoolean(EXTRA_FINISH_ON_COMPLETE, true);
  }

  @Override
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.video_surface, container, false);
    this.progress = view.findViewById(R.id.progress);
    final VideoView videoView = (VideoView)view.findViewById(R.id.video);
    this.videoView = videoView;
    videoView.setOnPreparedListener(this);
    videoView.setOnErrorListener(this);
    videoView.setOnCompletionListener(this);
    this.controller = new MediaController(getOwnerActivity());
    videoView.setMediaController(controller);
    return view;
  }

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    openVideo();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (!completed) {
      wakeLock.acquire();
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    getOwnerActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
  }

  @Override
  public void onPause() {
    super.onPause();
    if (wakeLock.isHeld()) {
      wakeLock.release();
    }
  }

  @Override
  public boolean onError(final MediaPlayer mp, final int what, final int extra) {
    Log.e(TAG, "Media player error " + what + "/" + extra);
    switch (what) {
    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
      openVideo();
      return true;
    default:
      new ErrorDialogFragment().show(getFragmentManager(), "error");
      return true;
    }
  }

  @Override
  public void onPrepared(final MediaPlayer mp) {
    if (progress != null) { progress.setVisibility(View.GONE); }
    mp.start();
  }

  @Override
  public void onCompletion(final MediaPlayer mp) {
    if (DEBUG) { Log.d(TAG, "Video playback completed"); }
    completed = true;
    if (wakeLock.isHeld()) {
      wakeLock.release();
    }
    if (finishOnComplete) { getOwnerActivity().finish(); }
  }

  private void openVideo() {
    if (progress != null) { progress.setVisibility(View.VISIBLE); }
    videoView.stopPlayback();
    final Uri uri = getArguments().<Uri>getParcelable(EXTRA_SOURCE_URI);
    videoView.setVideoURI(uri);
  }

  public void invalidateView() {
    final boolean show = controller != null && controller.isShowing();
    if (DEBUG) { Log.d(TAG, "Invalidate view, controller showing: " + show); }
    if (show) {
      controller.hide();
      // XXX don't know when we have new dimensions, it should be done with something like requestLayout
      final long delay = 250;
      handler.removeCallbacks(showController);
      handler.postDelayed(showController, delay);
    }
  }

  /**
   * Error dialog.
   * @author Roman Mazur (Stanfy - http://www.stanfy.com)
   */
  public static class ErrorDialogFragment extends BaseDialogFragment<Application> {

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
      return new AlertDialog.Builder(getOwnerActivity())
        .setMessage(R.string.video_error)
        .setCancelable(false)
        .setTitle(getOwnerActivity().getTitle())
        .setPositiveButton(R.string.ok, null)
        .create();
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
      super.onDismiss(dialog);
      getOwnerActivity().finish();
    }

  }

}
