package com.stanfy.app.service;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.stanfy.DebugFlags;

/**
 * Base application service which provides API and location methods interfaces.
 * @author Roman Mazur (Stanfy - http://www.stanfy.com)
 */
public class ApplicationService extends Service {

  /** Logging tag. */
  protected static final String TAG = "AppService";
  /** Debug flag. */
  protected static final boolean DEBUG = DebugFlags.DEBUG_SERVICES;

  /** Check for stop message. */
  private static final int MSG_CHECK_FOR_STOP = 1;

  /** Handler instance. */
  private Handler handler;

  /** API methods. */
  private ApiMethods apiMethods;
  /** Location methods. */
  private LocationMethodsImpl locationMethodsImpl;

  /** Usage flags. */
  private AtomicBoolean apiMethodsUse = new AtomicBoolean(false), locationMethodsUse = new AtomicBoolean(false);

  /** @return API methods implementation */
  protected ApiMethods createApiMethods() { return new ApiMethods(this); }

  /** @return location methods implementation */
  protected LocationMethodsImpl createLocationMethods() { return new LocationMethodsImpl(this); }

  @Override
  public void onCreate() {
    super.onCreate();
    handler = new InternalHandler(this);
    if (DEBUG) { Log.d(TAG, "Service created"); }
  }

  @Override
  public int onStartCommand(final Intent intent, final int flags, final int startId) {
    if (DEBUG) { Log.d(TAG, "Start command"); }
    handler.removeMessages(MSG_CHECK_FOR_STOP);
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    if (apiMethods != null) {
      apiMethods.destroy();
    }
    if (locationMethodsImpl != null) {
      locationMethodsImpl.destroy();
    }
    if (DEBUG) { Log.d(TAG, "Service destroyed"); }
    super.onDestroy();
  }

  @SuppressWarnings("deprecation")
  private void checkForLocationMethodsSupport(final boolean force) {
    if (locationMethodsImpl != null) { return; }
    boolean locationSupport = false;
    final Application app = getApplication();
    if (app instanceof com.stanfy.app.Application) {
      locationSupport = ((com.stanfy.app.Application) app).addLocationSupportToService();
    }
    if (force || locationSupport) {
      locationMethodsImpl = createLocationMethods();
    }
  }

  @Override
  public IBinder onBind(final Intent intent) {
    final String action = intent.getAction();
    if (DEBUG) { Log.d(TAG, "Binding to " + action); }
    if (action == null) { return null; }

    if (action.equals(ApiMethods.class.getName())) {
      apiMethodsUse.set(true);
      if (apiMethods == null) {
        apiMethods = createApiMethods();
      }
      checkForLocationMethodsSupport(false);
      return new ApiMethodsBinder(apiMethods);
    }
    if (action.equals(LocationMethods.class.getName())) {
      checkForLocationMethodsSupport(true);
      locationMethodsUse.set(true);
      return locationMethodsImpl.asBinder();
    }

    return null;
  }

  @Override
  public void onRebind(final Intent intent) {
    final String action = intent.getAction();
    if (action == null) { return; }
    if (DEBUG) { Log.d(TAG, "Rebinding to " + action); }
    if (action.equals(ApiMethods.class.getName())) {
      apiMethodsUse.set(true);
    } else if (action.equals(LocationMethods.class.getName())) {
      locationMethodsUse.set(true);
    }
  }

  @Override
  public boolean onUnbind(final Intent intent) {
    final String action = intent.getAction();
    if (action == null) { return false; }
    if (DEBUG) { Log.d(TAG, "Unbind from " + action); }
    if (apiMethods != null && action.equals(ApiMethods.class.getName())) {
      apiMethodsUse.set(false);
      checkForStop();
    } else if (locationMethodsImpl != null && action.equals(LocationMethods.class.getName())) {
      locationMethodsUse.set(false);
      checkForStop();
    }
    return true;
  }

  void checkForStop() {
    if (DEBUG) { Log.d(TAG, "Schedule check for stop"); }
    handler.removeMessages(MSG_CHECK_FOR_STOP);
    handler.sendEmptyMessage(MSG_CHECK_FOR_STOP);
  }

  protected void doStop() {
    stopSelf();
  }

  protected ApiMethods getApiMethods() { return apiMethods; }

  /** API methods binder. */
  public static class ApiMethodsBinder extends Binder {
    /** API methods. */
    private final ApiMethods apiMethods;

    public ApiMethodsBinder(final ApiMethods apiMethods) {
      this.apiMethods = apiMethods;
    }

    public ApiMethods getApiMethods() { return apiMethods; }
  }

  /** Internal handler. */
  protected static class InternalHandler extends Handler {

    /** Service instance. */
    private final WeakReference<ApplicationService> serviceRef;

    public InternalHandler(final ApplicationService service) {
      this.serviceRef = new WeakReference<ApplicationService>(service);
    }

    @Override
    public void handleMessage(final Message msg) {
      final ApplicationService service = serviceRef.get();
      if (service == null) { return; }

      switch (msg.what) {
      case MSG_CHECK_FOR_STOP:
        // here we decide whether to stop the service
        if (DEBUG) { Log.d(TAG, "Check for stop"); }
        final boolean hasUsers = service.apiMethodsUse.get() || service.locationMethodsUse.get();
        if (!hasUsers) {
          final boolean apiWorking = service.apiMethods != null && service.apiMethods.isWorking();
          if (!apiWorking) {
            if (DEBUG) { Log.d(TAG, "Stopping"); }
            service.doStop();
          }
        }
        break;
      default:
      }
    }

  }

}
