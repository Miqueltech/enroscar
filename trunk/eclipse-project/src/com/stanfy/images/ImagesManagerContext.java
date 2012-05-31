package com.stanfy.images;

import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import com.stanfy.images.ImagesManager.ImageHolder;
import com.stanfy.images.cache.ImageMemoryCache;
import com.stanfy.images.cache.SoftImageMemoryCache;
import com.stanfy.images.cache.StaticSizeImageMemoryCache;
import com.stanfy.images.cache.SupportLruImageMemoryCache;
import com.stanfy.images.model.CachedImage;

/**
 * Images manager context.
 * Supported views: {@link android.widget.ImageView}, {@link android.widget.CompoundButton}, {@link android.widget.TextView}.
 * @param <T> cached image type
 * @author Roman Mazur - Stanfy (http://www.stanfy.com)
 */
@SuppressWarnings("deprecation")
public class ImagesManagerContext<T extends CachedImage> {

  /** Images DAO. */
  private ImagesDAO<T> imagesDAO;
  /** Downloader. */
  private Downloader downloader;
  /** Images manager. */
  private ImagesManager<T> imagesManager;

  /**
   * Memcache modes.
   * @author Roman Mazur (Stanfy - http://www.stanfy.com)
   */
  public static enum MemCacheMode {
    /**
     * Based on hash map.
     * @deprecated use {@link #LRU} instead
     */
    @Deprecated
    STATIC {
      @Override
      public ImageMemoryCache createCache() {
        return new StaticSizeImageMemoryCache();
      }
    },
    /**
     * Based on soft references.
     * @deprecated use {@link #LRU} instead
     */
    @Deprecated
    SOFT {
      @Override
      public ImageMemoryCache createCache() {
        return new SoftImageMemoryCache();
      }
    },
    /** Based on LRU cache class. */
    LRU {
      @Override
      public ImageMemoryCache createCache() {
        return new SupportLruImageMemoryCache();
      }
    };

    public abstract ImageMemoryCache createCache();
  }

  /** @param count count image loading of executors */
  public static void configureExecutorsCount(final int count) {
    Threading.configureImageTasksExecutor(count);
  }

  public static boolean check(final Uri uri) {
    return uri == null || (uri.getScheme() != null
        && (uri.getScheme().startsWith("http")
            || uri.getScheme().startsWith("content")
            || uri.getScheme().startsWith("file")));
  }

  /** @return the imagesDAO */
  public ImagesDAO<T> getImagesDAO() { return imagesDAO; }
  /** @param imagesDAO the imagesDAO to set */
  public void setImagesDAO(final ImagesDAO<T> imagesDAO) { this.imagesDAO = imagesDAO; }
  /** @return the downloader */
  public Downloader getDownloader(final ImageHolder holder, final String url) {
    if (url != null && !url.startsWith("http")) {
      return new ContentUriDownloader(holder.getContext().getApplicationContext().getContentResolver());
    }
    return this.downloader;
  }
  /** @param downloader the downloader to set */
  public void setDownloader(final Downloader downloader) { this.downloader = downloader; }
  /** @return the imagesManager */
  public ImagesManager<T> getImagesManager() { return imagesManager; }
  /** @param imagesManager the imagesManager to set */
  public void setImagesManager(final ImagesManager<T> imagesManager) { this.imagesManager = imagesManager; }

  /** @param mode memcache mode */
  public void setMemCache(final MemCacheMode mode) {
    if (mode == null) {
      throw new IllegalArgumentException("Mem cache mode cannot be null");
    }
    imagesManager.setMemCache(mode.createCache());
  }

  public void ensureImages(final Context context, final List<T> images) {
    imagesManager.ensureImages(imagesDAO, downloader, context, images);
  }

  /**
   * Cancel loading for a view.
   * @param view the view to manipulate
   */
  public void cancel(final View view) {
    final ImagesManager<T> imagesManager = this.imagesManager;
    if (imagesManager != null) { imagesManager.cancelImageLoading(view); }
  }

  public void populate(final View view, final String url) {
    final ImagesDAO<T> imagesDAO = this.imagesDAO;
    final Downloader downloader = this.downloader;
    final ImagesManager<T> imagesManager = this.imagesManager;
    if (imagesDAO != null && downloader != null && imagesManager != null) {
      imagesManager.populateImage(view, url, this);
    }
  }

  public void populate(final ImageHolder imageHolder, final String url) {
    final ImagesDAO<T> imagesDAO = this.imagesDAO;
    final Downloader downloader = this.downloader;
    final ImagesManager<T> imagesManager = this.imagesManager;
    if (imagesDAO != null && downloader != null && imagesManager != null) {
      imagesManager.populateImage(imageHolder, url, this);
    }
  }

  public void cancel(final ImageHolder imageHolder) {
    final ImagesManager<T> imagesManager = this.imagesManager;
    if (imagesManager != null) { imagesManager.cancelImageLoading(imageHolder); }
  }

  /**
   * Flush resources.
   */
  public void flush() {
    if (imagesManager != null) {
      imagesManager.flush();
    }
  }

}
