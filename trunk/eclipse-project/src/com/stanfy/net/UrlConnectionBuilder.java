package com.stanfy.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.net.Uri;

import com.stanfy.net.cache.CacheControlUrlConnection;
import com.stanfy.serverapi.response.ModelTypeToken;

/**
 * Builder for creating URL connections.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 */
public class UrlConnectionBuilder {

  /** URL to create a connection. */
  private URL url;

  /** Cache manager name. */
  private String cacheManagerName;
  /** Content handler name. */
  private String contentHandlerName;
  /** Model type. */
  private ModelTypeToken modelType;

  public UrlConnectionBuilder setUrl(final URL url) {
    this.url = url;
    return this;
  }

  public UrlConnectionBuilder setUrl(final String url) {
    try {
      this.url = new URL(url);
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException("Bad URL string: " + url, e);
    }
    return this;
  }

  public UrlConnectionBuilder setUrl(final Uri url) {
    return this.setUrl(url.toString());
  }

  public UrlConnectionBuilder setCacheManagerName(final String cacheManagerName) {
    this.cacheManagerName = cacheManagerName;
    return this;
  }

  public UrlConnectionBuilder setContentHandlerName(final String contentHandlerName) {
    this.contentHandlerName = contentHandlerName;
    return this;
  }

  public UrlConnectionBuilder setModelType(final ModelTypeToken modelType) {
    this.modelType = modelType;
    return this;
  }

  public URLConnection create() throws IOException {
    URLConnection connection = url.openConnection();
    // cache
    if (cacheManagerName != null) {
      connection.setUseCaches(true); // core
      connection = new CacheControlUrlConnection(connection, cacheManagerName);
      connection.setUseCaches(true); // wrapper
    }
    // content handler
    if (contentHandlerName != null || modelType != null) {
      final ContentControlUrlConnection control = new ContentControlUrlConnection(connection);
      control.setModelType(modelType);
      control.setContentHandlerName(contentHandlerName);
      connection = control;
    }
    return connection;
  }

}