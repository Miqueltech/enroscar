package com.stanfy.app.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import com.stanfy.DebugFlags;
import com.stanfy.Destroyable;
import com.stanfy.app.Application;
import com.stanfy.app.service.ApiMethods.Stub;
import com.stanfy.app.service.serverapi.RequestDescriptionProcessor;
import com.stanfy.app.service.serverapi.RequestDescriptionProcessor.RequestProcessorHooks;
import com.stanfy.serverapi.RequestMethod;
import com.stanfy.serverapi.request.Operation;
import com.stanfy.serverapi.request.RequestDescription;
import com.stanfy.serverapi.response.ParserContext;
import com.stanfy.serverapi.response.RequestConfigurableContext;
import com.stanfy.serverapi.response.ResponseData;
import com.stanfy.utils.AppUtils;

/**
 * Implementation for {@link ApiMethods}.
 * <p>
 *   There are two options how to handle incoming remote API request:
 *   <ol>
 *     <li>enqueue it so that incoming requests are processed one by one in a separate thread in FIFO order</li>
 *     <li>run it in parallel to other requests</li>
 *   </ol>
 * </p>
 * @author Roman Mazur - Stanfy (http://www.stanfy.com)
 */
public class ApiMethodsImpl extends Stub implements Destroyable {

  /** Logging tag. */
  static final String TAG = "ApiMethodsImpl";
  /** Debug flag. */
  private static final boolean DEBUG = DebugFlags.DEBUG_SERVICES;

  /** Main worker thread. */
  private static final String THREAD_NAME = "remote-api-thread";

  /** Main worker. */
  private static final HandlerThread MAIN_WORKER = new HandlerThread(THREAD_NAME);
  static {
    MAIN_WORKER.start();
  }

  /** Message code. */
  private static final int MSG_REQUEST = 0, // make a request
                           MSG_FINISH = 1;  // all requests are done

  /** Null operation data. */
  static final APICallInfoData NULL_OPERATION_DATA = new APICallInfoData();

  /**
   * @author Roman Mazur (Stanfy - http://www.stanfy.com)
   */
  private abstract static class CallbackReporter {
    /** Reporter name. */
    final String name;
    protected CallbackReporter(final String name) {
      this.name = name;
    }
    abstract void report(final ApiMethodCallback callback, final int token, final int operation, final ResponseData responseData) throws RemoteException;
  }

  /** Calls {@link ApiMethodCallback#reportSuccess(int, int, String, Uri, ResponseData)}. */
  private static final CallbackReporter SUCCESS_REPORTER = new CallbackReporter("success") {
    @Override
    void report(final ApiMethodCallback callback, final int token, final int operation, final ResponseData responseData) throws RemoteException {
      callback.reportSuccess(token, operation, responseData);
    }
  };
  /** Calls {@link ApiMethodCallback#reportSuccess(int, int, String, Uri, ResponseData)}. */
  private static final CallbackReporter ERROR_REPORTER = new CallbackReporter("error") {
    @Override
    void report(final ApiMethodCallback callback, final int token, final int operation, final ResponseData responseData) throws RemoteException {
      callback.reportError(token, operation, responseData);
    }
  };

  /** Special handler. */
  protected class ApiMethodsHandler extends Handler {

    public ApiMethodsHandler(final Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(final Message msg) {
      final ApplicationService appService = ApiMethodsImpl.this.appService;
      if (appService == null) { return; } // we are destroyed

      if (msg.what == MSG_FINISH) {
        appService.checkForStop();
        return;
      }

      try {
        initSync.await();
      } catch (final InterruptedException e) {
        Log.e(TAG, "Worker was interrupted", e);
        appService.checkForStop();
        return;
      }

      activeWorkersCount.incrementAndGet();
      rdProcessor.process(appService, (RequestDescription)msg.obj, queuedProcessorHooks);
      activeWorkersCount.decrementAndGet();
    }

  }

  /**
   * Async task that performs a request.
   * @author Roman Mazur (Stanfy - http://stanfy.com)
   */
  protected class AsyncRequestTask extends AsyncTask<RequestDescription, Void, Void> {
    @Override
    protected void onPreExecute() {
      activeWorkersCount.incrementAndGet();
    }
    @Override
    protected Void doInBackground(final RequestDescription... params) {
      rdProcessor.process(appService, params[0], parallelProcessorHooks);
      return null;
    }
    @Override
    protected void onPostExecute(final Void result) {
      activeWorkersCount.decrementAndGet();
      mainHandler.sendEmptyMessage(MSG_FINISH);
    }
  }

  /** Handler instance for main worker. */
  private final ApiMethodsHandler mainHandler;

  /** Working flag. */
  final AtomicInteger activeWorkersCount = new AtomicInteger(0);
  /** Initialization sync point. */
  final CountDownLatch initSync = new CountDownLatch(1);

  /** Request description processing strategy. */
  final RequestDescriptionProcessor rdProcessor;
  /** Processor hooks. */
  private final RequestProcessorHooks queuedProcessorHooks, parallelProcessorHooks;

  /** Application service. */
  final ApplicationService appService;

  /** API callbacks. */
  private final RemoteCallbackList<ApiMethodCallback> apiCallbacks = new RemoteCallbackList<ApiMethodCallback>();

  /** Last operation dump. */
  private final SharedPreferences lastOperationStore;

  /** Operations info. */
  final APICallInfoData pending = new APICallInfoData(), lastOperation = new APICallInfoData();

  /**
   * Main hooks implementation. Performs request callbacks reporting and optional configuration for {@link RequestConfigurableContext}s.
   * @author Roman Mazur (Stanfy - http://stanfy.com)
   */
  protected class MainHooks implements RequestProcessorHooks {
    @Override
    public void beforeRequestProcessingStarted(final RequestDescription requestDescription, final ParserContext pContext, final RequestMethod requestMethod) {
      if (pContext instanceof RequestConfigurableContext) {
        ((RequestConfigurableContext)pContext).configureContext(requestDescription, appService);
      }
    }
    @Override
    public void afterRequestProcessingFinished(final RequestDescription requestDescription, final ParserContext pContext, final RequestMethod requestMethod) {
    }

    @Override
    public void onRequestSuccess(final RequestDescription requestDescription, final ResponseData responseData) {
      reportApiSuccess(requestDescription.getOperationCode(), requestDescription.getToken(), responseData);
    }
    @Override
    public void onRequestError(final RequestDescription requestDescription, final ResponseData responseData) {
      reportError(requestDescription.getOperationCode(), requestDescription.getToken(), responseData);
    }
  }

  /**
   * Hooks used by {@link ApiMethodsHandler}.
   * @author Roman Mazur (Stanfy - http://stanfy.com)
   */
  private class QueueRequestHooks implements RequestProcessorHooks {
    /** Main hooks. */
    private final RequestProcessorHooks mainHooks;

    public QueueRequestHooks(final RequestProcessorHooks mainHooks) {
      this.mainHooks = mainHooks;
    }

    @Override
    public void beforeRequestProcessingStarted(final RequestDescription requestDescription, final ParserContext pContext, final RequestMethod requestMethod) {

      mainHooks.beforeRequestProcessingStarted(requestDescription, pContext, requestMethod);

      pending.set(NULL_OPERATION_DATA);
      pending.set(requestDescription.getOperationCode(), requestDescription.getToken());

    }
    @Override
    public void afterRequestProcessingFinished(final RequestDescription requestDescription, final ParserContext pContext, final RequestMethod requestMethod) {

      mainHooks.afterRequestProcessingFinished(requestDescription, pContext, requestMethod);

      if (DEBUG) { Log.d(TAG, "Dump " + lastOperation.operation); }
      lastOperation.save(lastOperationStore);
      pending.set(NULL_OPERATION_DATA);

    }

    @Override
    public void onRequestSuccess(final RequestDescription requestDescription, final ResponseData responseData) {
      mainHooks.onRequestSuccess(requestDescription, responseData);
    }
    @Override
    public void onRequestError(final RequestDescription requestDescription, final ResponseData responseData) {
      mainHooks.onRequestError(requestDescription, responseData);
    }
  }

  /**
   * Constructs remote API methods implementation.<br/>
   * It creates instances of {@link RequestDescriptionProcessor} and {@link RequestProcessorHooks}.
   * @param appService application service
   */
  public ApiMethodsImpl(final ApplicationService appService) {
    this.appService = appService;

    this.lastOperationStore = appService.getSharedPreferences("last-operation", Context.MODE_PRIVATE);
    loadLastOperation();

    this.parallelProcessorHooks = createRequestDescriptionHooks();
    this.queuedProcessorHooks = new QueueRequestHooks(this.parallelProcessorHooks);
    this.rdProcessor = createRequestDescriptionProcessor(appService.getApp());

    mainHandler = createApiMethodsHandler(MAIN_WORKER.getLooper());
    if (DEBUG) { Log.d(TAG, "Worker thread is now alive " + this); }
  }

  /**
   * This method starts asynchronous read of last operation.
   * Workers must be synchronized with this reading.
   */
  private void loadLastOperation() {
    AppUtils.getSdkDependentUtils().executeAsyncTaskParallel(
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(final Void... params) {
          lastOperation.load(lastOperationStore);
          initSync.countDown();
          return null;
        }
      }
    );
  }

  /**
   * Constructs a handler for processing the queue of remote API requests.
   * @param looper looper instance that must be passed to the handler constructor
   * @return main worker thread handler
   */
  protected ApiMethodsHandler createApiMethodsHandler(final Looper looper) { return new ApiMethodsHandler(looper); }
  /**
   * Constructor a request description processing strategy.
   * @param app application instance
   * @return processor instance
   */
  protected RequestDescriptionProcessor createRequestDescriptionProcessor(final Application app) { return new RequestDescriptionProcessor(app); }
  /**
   * @return request description processing hooks
   */
  protected RequestProcessorHooks createRequestDescriptionHooks() { return new MainHooks(); }

  private void updateLastOperation(final ResponseData rd) {
    final APICallInfoData lastOperation = this.lastOperation;
    lastOperation.set(pending);
    lastOperation.set(rd);
  }

  private synchronized void reportToCallbacks(final int token, final int opCode, final ResponseData responseData, final CallbackReporter reporter) {
    updateLastOperation(responseData);
    if (DEBUG) { Log.v(TAG, "Start broadcast"); }
    int c = apiCallbacks.beginBroadcast();
    ResponseData noModelData = null;
    while (c > 0) {
      --c;
      try {
        final ApiMethodCallback callback = apiCallbacks.getBroadcastItem(c);
        if (DEBUG) { Log.d(TAG, "Report API " + reporter.name + "/op=" + opCode + "/token=" + token + " " + c + ": " + callback); }
        final boolean requiresModel = (Boolean)apiCallbacks.getBroadcastCookie(c);
        ResponseData sendingData = responseData;
        if (!requiresModel) {
          if (noModelData == null) { noModelData = ResponseData.withoutModel(responseData); }
          sendingData = noModelData;
        }
        reporter.report(callback, token, opCode, sendingData);
      } catch (final RemoteException e) {
        Log.e(TAG, "Cannot run callback report method", e);
      }
    }
    apiCallbacks.finishBroadcast();
    if (DEBUG) { Log.v(TAG, "Finish broadcast"); }
  }

  void reportApiSuccess(final int opCode, final int token, final ResponseData responseData) {
    reportToCallbacks(token, opCode, responseData, SUCCESS_REPORTER);
  }

  void reportError(final int opCode, final int token, final ResponseData responseData) {
    reportToCallbacks(token, opCode, responseData, ERROR_REPORTER);
  }

  boolean isWorking() { return activeWorkersCount.intValue() == 0; }

  @Override
  public void destroy() {
    apiCallbacks.kill();
    if (DEBUG) { Log.d(TAG, "API methods destroyed"); }
  }


  // --------------------------------------------------------------------------------------------

  @Override
  public void performRequest(final RequestDescription description) throws RemoteException {
    final Handler handler = this.mainHandler;
    if (handler == null) { return; }
    if (DEBUG) { Log.d(TAG, "Perform " + description + " " + this); }
    if (description.isParallelMode()) {
      new AsyncRequestTask().execute(description);
    } else {
      handler.removeMessages(MSG_FINISH);
      handler.sendMessage(handler.obtainMessage(MSG_REQUEST, description));
      handler.sendEmptyMessage(MSG_FINISH);
    }
  }

  @Override
  public void registerCallback(final ApiMethodCallback callback, final boolean requiresModel) throws RemoteException {
    if (DEBUG) { Log.d(TAG, "Register API callback " + callback); }
    final APICallInfoData b = new APICallInfoData();
    b.set(lastOperation);
    if (b.hasData()) {
      if (DEBUG) { Log.d(TAG, "Report last operation " + b.operation); }
      callback.reportLastOperation(b.token, b.operation, b.responseData);
    }
    b.set(pending);
    if (b.hasData()) { callback.reportPending(b.token, b.operation); }
    apiCallbacks.register(callback, requiresModel);
  }

  @Override
  public void removeCallback(final ApiMethodCallback callback) throws RemoteException {
    if (DEBUG) { Log.d(TAG, "Remove API callback " + callback); }
    apiCallbacks.unregister(callback);
  }

  // --------------------------------------------------------------------------------------------

  /**
   * Information about last operation.
   * @author Roman Mazur (Stanfy - http://www.stanfy.com)
   */
  static class APICallInfoData {

    /** Preference key names. */
    private static final String OP_CODE = "op", TOKEN = "token",
                                RD_MESSAGE = "msg", RD_ERROR = "errorCode", RD_DATA = "data";

    /** Operation code. */
    int operation = Operation.NOP;
    /** Operation token. */
    int token = -1;
    /** Response data. */
    ResponseData responseData = new ResponseData();

    public void set(final APICallInfoData data) {
      this.operation = data.operation;
      this.token = data.token;
      responseData = data.responseData;
    }
    public void set(final ResponseData rd) {
      final ResponseData responseData = this.responseData;
      responseData.setErrorCode(rd.getErrorCode());
      responseData.setData(rd.getData());
      responseData.setMessage(rd.getMessage());
    }
    public void set(final int opCode, final int token) {
      this.operation = opCode;
      this.token = token;
    }
    public boolean hasData() { return operation != Operation.NOP; }

    public void save(final SharedPreferences preferences) {
      final Editor lastOperationEditor = preferences.edit();
      lastOperationEditor
          .putInt(OP_CODE, this.operation)
          .putInt(TOKEN, this.token);
      final ResponseData rd = this.responseData;
      if (rd != null) {
        final Uri dataUri = rd.getData();
        lastOperationEditor
          .putString(RD_MESSAGE, rd.getMessage())
          .putInt(RD_ERROR, rd.getErrorCode())
          .putString(RD_DATA, dataUri != null ? dataUri.toString() : null);
      }
      lastOperationEditor.commit();
    }

    public void load(final SharedPreferences preferences) {
      final SharedPreferences src = preferences;
      final APICallInfoData dst = this;
      dst.set(src.getInt(OP_CODE, Operation.NOP), src.getInt(TOKEN, -1));
      final ResponseData responseData = new ResponseData();
      responseData.setMessage(src.getString(RD_MESSAGE, null));
      responseData.setErrorCode(src.getInt(RD_ERROR, ResponseData.RESPONSE_CODE_ILLEGAL));
      final String url = src.getString(RD_DATA, null);
      responseData.setData(url != null ? Uri.parse(url) : null);
      dst.set(responseData);
      if (DEBUG) { Log.d(TAG, "Loaded last operation: " + dst.operation + " / " + dst.responseData.getErrorCode() + " -> " + dst.hasData()); }
    }

  }

}
