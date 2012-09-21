package com.stanfy.app.beans;

import java.net.ContentHandler;
import java.net.ResponseCache;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.Log;

import com.stanfy.DebugFlags;
import com.stanfy.app.ActivityBehaviorFactory;
import com.stanfy.app.CrucialGUIOperationManager;
import com.stanfy.content.AppDatabaseManager;
import com.stanfy.images.ImagesManager;
import com.stanfy.images.cache.ImageFileCache;
import com.stanfy.images.cache.ImageMemoryCache;
import com.stanfy.images.cache.SupportLruImageMemoryCache;
import com.stanfy.io.BuffersPool;
import com.stanfy.serverapi.RemoteServerApiConfiguration;
import com.stanfy.serverapi.request.SimpleRequestBuilder;
import com.stanfy.serverapi.response.handler.GsonContentHandler;
import com.stanfy.serverapi.response.handler.StringContentHandler;
import com.stanfy.serverapi.response.handler.XmlGsonContentHandler;
import com.stanfy.stats.EmptyStatsManager;
import com.stanfy.stats.StatsManager;
import com.stanfy.utils.AppUtils;
import com.stanfy.utils.sdk.SDKDependentUtilsFactory;

/**
 * Beans manager.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 */
public class BeansManager {

  /** Debug flag. */
  private static final boolean DEBUG = DebugFlags.DEBUG_GUI;

  /** Logging tag. */
  private static final String TAG = "Beans";

  /** Old SDK flag. */
  private static final boolean OLD_SDK = VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH;

  /** Singleton instance. */
  private static BeansManager instance;

  /** Application instance. */
  private final Application application;

  /** Container. */
  private final BeansContainer container;

  /** Register callbacks flag. */
  private boolean callbacksRegistered = false;

  protected BeansManager(final Application application) {
    this.application = application;
    this.container = createContainer(application);
  }

  public static synchronized BeansManager get(final Context context) {
    if (instance == null) {
      if (context == null) { return null; }
      instance = new BeansManager((Application)context.getApplicationContext());
    }
    return instance;
  }

  /**
   * Call this method from {@link Application#onLowMemory()} to integrate enroscar on pre-ICS versions.
   * @param context application context
   */
  public static void onLowMemory(final Context context) {
    if (OLD_SDK) {
      get(context).container.onLowMemory();
    }
  }
  /**
   * Call this method from {@link Application#onConfigurationChanged()} to integrate enroscar on pre-ICS versions.
   * @param context application context
   * @param newConfig new configuration
   */
  public static void onConfigurationChanged(final Context context, final Configuration newConfig) {
    if (OLD_SDK) {
      get(context).container.onConfigurationChanged(newConfig);
    }
  }

  /**
   * Integrate with component callbacks. It calls {@link Application#registerComponentCallbacks(android.content.ComponentCallbacks)},
   * so that it's useless on prior ICS SDKs.
   */
  void registerComponentCallbacks() {
    if (callbacksRegistered) { return; } // do it once only
    AppUtils.getSdkDependentUtils().registerComponentCallbacks(application, this.container);
    callbacksRegistered = true;
  }

  /**
   * @param application application instance
   * @return beans container implementation
   */
  protected BeansContainer createContainer(final Application application) {
    return new DefaultBeansContainer();
  }

  public Application getApplication() { return application; }
  public BeansContainer getContainer() { return container; }

  /** @return beans editor instance */
  public Editor edit() { return new Editor(); }

  /** @return images manager instance */
  public ImagesManager getImagesManager() { return container.getBean(ImagesManager.BEAN_NAME, ImagesManager.class); }
  /** @return main buffers pool instance */
  public BuffersPool getMainBuffersPool() { return container.getBean(BuffersPool.BEAN_NAME, BuffersPool.class); }
  /** @return image memory cache instance */
  public ImageMemoryCache getImageMemoryCache() { return container.getBean(ImageMemoryCache.BEAN_NAME, ImageMemoryCache.class); }
  /** @return response cache instance */
  public ResponseCache getResponseCache(final String name) { return container.getBean(name, ResponseCache.class); }
  /** @return crucial GUI operation manager */
  public CrucialGUIOperationManager getCrucialGUIOperationManager() { return container.getBean(CrucialGUIOperationManager.BEAN_NAME, CrucialGUIOperationManager.class); }
  /** @return activity behavior factory */
  public ActivityBehaviorFactory getActivityBehaviorFactory() { return container.getBean(ActivityBehaviorFactory.BEAN_NAME, ActivityBehaviorFactory.class); }
  /** @return statistics manager */
  public StatsManager getStatsManager() { return container.getBean(StatsManager.BEAN_NAME, StatsManager.class); }
  /** @return SDK dependent utilities factory */
  public SDKDependentUtilsFactory getSdkDependentUtilsFactory() { return container.getBean(SDKDependentUtilsFactory.BEAN_NAME, SDKDependentUtilsFactory.class); }
  /** @return remote server API access configuration */
  public RemoteServerApiConfiguration getRemoteServerApiConfiguration() { return container.getBean(RemoteServerApiConfiguration.BEAN_NAME, RemoteServerApiConfiguration.class); }
  /** @return content handler instance */
  public ContentHandler getContentHandler(final String name) { return container.getBean(name, ContentHandler.class); }
  /** @return application database manager instance */
  public AppDatabaseManager getAppDatabaseManager() { return container.getBean(AppDatabaseManager.class); }

  /**
   * Beans editor.
   */
  public class Editor {
    /** Container instance. */
    final BeansContainer container = getContainer();

    /** Editor actions. */
    private final LinkedHashMap<String, Runnable> editorActions = new LinkedHashMap<String, Runnable>();

    void checkIntrfaces(final Object bean) {
      if (bean instanceof ManagerAwareBean) {
        ((ManagerAwareBean) bean).setBeansManager(BeansManager.this);
      }
    }

    public <T> Editor put(final String name, final T bean) {
      editorActions.put(name, new Runnable() {
        @Override
        public void run() {
          container.putEntityInstance(name, bean);
          checkIntrfaces(bean);
        }
      });
      return this;
    }
    public <T> Editor put(final Class<T> beanClass) {
      final EnroscarBean info = AppUtils.getBeanInfo(beanClass);
      editorActions.put(info.value(), new Runnable() {
        @Override
        public void run() {
          final T bean = container.putEntityInstance(beanClass, application);
          checkIntrfaces(bean);
        }
      });
      return this;
    }
    public <T> Editor put(final T bean) {
      final EnroscarBean info = AppUtils.getBeanInfo(bean.getClass());
      editorActions.put(info.value(), new Runnable() {
        @Override
        public void run() {
          container.putEntityInstance(bean);
          checkIntrfaces(bean);
        }
      });
      return this;
    }

    public Editor required() {
      put(SDKDependentUtilsFactory.class);
      put(BuffersPool.class);
      put(EmptyStatsManager.class);
      return this;
    }

    public Editor images() {
      put(ImageFileCache.class);
      put(SupportLruImageMemoryCache.class);
      put(ImagesManager.class);
      return this;
    }

    public Editor activitiesBehavior() {
      put(ActivityBehaviorFactory.class);
      put(CrucialGUIOperationManager.class);
      return this;
    }

    public Editor remoteServerApi(final String... formats) {
      put(RemoteServerApiConfiguration.class);
      if (formats.length > 0) {

        Class<?> mainClass = null;
        for (final String format : formats) {
          Class<?> handlerClass = null;

          if (SimpleRequestBuilder.JSON.equals(format)) {
            handlerClass = GsonContentHandler.class;
          } else if (SimpleRequestBuilder.XML.equals(format)) {
            handlerClass = XmlGsonContentHandler.class;
          } else if (SimpleRequestBuilder.STRING.equals(format)) {
            handlerClass = StringContentHandler.class;
          }

          if (handlerClass == null) {
            throw new RuntimeException("Unknown format " + format);
          }

          if (mainClass == null) { mainClass = handlerClass; }
          put(handlerClass);
        }

        final String mainContentHandlerName = AppUtils.getBeanInfo(mainClass).value();
        editorActions.put("remoteServerApi-config", new Runnable() {
          @Override
          public void run() {
            getContainer().getBean(RemoteServerApiConfiguration.BEAN_NAME, RemoteServerApiConfiguration.class)
              .setDefaultContentHandlerName(mainContentHandlerName);
          }
        });

      }
      return this;
    }

    public Editor defaults() {
      required();
      images();
      activitiesBehavior();
      remoteServerApi();
      return this;
    }

    public void commit() {
      final long start = System.currentTimeMillis();
      for (final Entry<String, Runnable> entry : editorActions.entrySet()) {
        final long startAction = System.currentTimeMillis();
        entry.getValue().run();
        if (DEBUG) { Log.d(TAG, "One action time: " + (System.currentTimeMillis() - startAction)); }
      }
      if (DEBUG) { Log.d(TAG, "Run actions time: " + (System.currentTimeMillis() - start)); }
      registerComponentCallbacks();
      if (DEBUG) { Log.d(TAG, "Before init time: " + (System.currentTimeMillis() - start)); }
      container.triggerInitFinished();
      if (DEBUG) { Log.d(TAG, "All commit time: " + (System.currentTimeMillis() - start)); }
    }

  }

}
