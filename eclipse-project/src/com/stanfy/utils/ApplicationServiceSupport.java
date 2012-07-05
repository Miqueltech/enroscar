package com.stanfy.utils;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;

import com.stanfy.DebugFlags;
import com.stanfy.app.beans.BeansManager;
import com.stanfy.serverapi.RemoteServerApiConfiguration;

/**
 * Base class for helpers that can bind to application service.
 * @param <T> service object type
 * @author Roman Mazur - Stanfy (http://www.stanfy.com)
 */
abstract class ApplicationServiceSupport<T> implements ServiceConnection {

  /** Logging tag. */
  private static final String TAG = "ServiceSupport";
  /** Debug flag. */
  private static final boolean DEBUG = DebugFlags.DEBUG_SERVICES, DEBUG_CALLS = false;

  /** Context instance. */
  final WeakReference<Context> contextRef;

  /** Configuration. */
  final RemoteServerApiConfiguration configuration;

  /** Service object. */
  T serviceObject;

  public ApplicationServiceSupport(final Context a) {
    this.contextRef = new WeakReference<Context>(a);
    this.configuration = BeansManager.get(a).getRemoteServerApiConfiguration();
  }

  protected abstract Class<T> getInterfaceClass();

  /**
   * Create the service connection.
   */
  public void bind() {
    if (serviceObject != null) { return; }
    final Context context = contextRef.get();
    if (context == null) { return; }
    final Intent intent = new Intent(context, configuration.getApplicationServiceClass());
    intent.setAction(getInterfaceClass().getName());
    if (DEBUG_CALLS) {
      Log.v(TAG, "Attempt to bind to service " + this + "/" + context, new RuntimeException());
    }
    // start manually, so that it will be stopped manually
    context.startService(intent);
    final boolean bindResult = context.bindService(intent, this, 0);
    if (DEBUG) { Log.v(TAG, "Binded to service: " + bindResult + ", " + context + ", interface: " + getInterfaceClass()); }
  }

  /**
   * Destroy the service connection.
   */
  public void unbind() {
    if (serviceObject == null) { return; }
    serviceObject = null;
    final Context context = contextRef.get();
    if (DEBUG) { Log.v(TAG, "Unbind " + context + " from " + getInterfaceClass()); }
    if (context == null) { return; }
    try {
      context.unbindService(this);
    } catch (final Exception e) {
      if (DEBUG) { Log.e(TAG, "Cannot unbind from application service", e); }
    }
  }

}
