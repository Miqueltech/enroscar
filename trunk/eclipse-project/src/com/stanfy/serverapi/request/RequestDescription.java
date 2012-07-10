package com.stanfy.serverapi.request;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import com.stanfy.DebugFlags;
import com.stanfy.io.IoUtils;
import com.stanfy.serverapi.request.binary.BinaryData;
import com.stanfy.serverapi.request.net.BaseRequestDescriptionConverter;
import com.stanfy.serverapi.request.net.SimpleGetConverter;
import com.stanfy.serverapi.request.net.SimplePostConverter;
import com.stanfy.serverapi.request.net.UploadPostConverter;
import com.stanfy.serverapi.response.ModelTypeToken;

/**
 * Request method description. This object is passed to the service describing the request.
 * @author Roman Mazur - Stanfy (http://www.stanfy.com)
 */
public class RequestDescription implements Parcelable {

  /** Default name for binary content. */
  public static final String BINARY_NAME_DEFAULT = "content";

  /** ID counter. */
  private static int idCounter = 0;

  /** Logging tag. */
  public static final String TAG = "ReqDesc";

  /** Charset. */
  public  static final String CHARSET = "UTF-8";

  /** Debug flag. */
  public static final boolean DEBUG = DebugFlags.DEBUG_API;

  /** Creator. */
  public static final Creator<RequestDescription> CREATOR = new Creator<RequestDescription>() {
    @Override
    public RequestDescription createFromParcel(final Parcel source) { return new RequestDescription(source); }
    @Override
    public RequestDescription[] newArray(final int size) { return new RequestDescription[size]; }
  };

  private static synchronized int nextId() {
    ++idCounter;
    if (idCounter < 0) { idCounter = 1; }
    return idCounter;
  }

  /** Converters to {@link URLConnection}. */
  private static final SparseArray<BaseRequestDescriptionConverter> CONVERTERS = new SparseArray<BaseRequestDescriptionConverter>(3);
  static {
    CONVERTERS.put(OperationType.SIMPLE_GET, new SimpleGetConverter());
    CONVERTERS.put(OperationType.SIMPLE_POST, new SimplePostConverter());
    CONVERTERS.put(OperationType.UPLOAD_POST, new UploadPostConverter());
  }

  /** Request ID. */
  final int id;

  /** Operation type. */
  int operationType = OperationType.SIMPLE_GET;

  /** URL part. */
  String url;
  /** Cache instance name. */
  String cacheName;
  /** Simple parameters. */
  ParametersGroup simpleParameters;

  /** Content type. */
  String contentType;
  /** Request data encoding. */
  Charset encoding = IoUtils.UTF_8;
  /** Content language. */
  String contentLanguage;

  /** Meta information. */
  ParametersGroup metaParameters;

  /** Binary data array. */
  ArrayList<BinaryData<?>> binaryData;

  /** Whether request should be performed in parallel. */
  boolean parallelMode = false;

  /** Canceled state flag. */
  volatile boolean canceled = false;

  /** Model class. */
  ModelTypeToken modelType;
  /** Content handler name. */
  String contentHandler;
  /** Content analyzer. */
  String contentAnalyzer;

  public static String getParamValue(final String name, final LinkedList<Parameter> param) {
    for (final Parameter p : param) {
      if (p instanceof ParameterValue && name.equals(p.getName())) {
        return ((ParameterValue)p).getValue();
      }
    }
    return null;
  }

  /**
   * Create with predefined ID.
   * @param id request ID
   */
  public RequestDescription(final int id) {
    this.id = id;
  }

  /**
   * Create new description with new request ID.
   */
  public RequestDescription() {
    this(nextId());
  }

  /**
   * Create from parcel.
   */
  protected RequestDescription(final Parcel source) {
    this(source.readInt());
    this.operationType = source.readInt();
    this.url = source.readString();
    this.cacheName = source.readString();
    this.simpleParameters = source.readParcelable(null);
    this.contentType = source.readString();
    this.encoding = Charset.forName(source.readString());
    this.contentLanguage = source.readString();
    this.metaParameters = source.readParcelable(null);
    this.parallelMode = source.readInt() == 1;
    this.canceled = source.readInt() == 1;

    this.modelType = source.readParcelable(null);
    this.contentHandler = source.readString();
    this.contentAnalyzer = source.readString();

    // binary content fields
    final BinaryData<?>[] binary = (BinaryData<?>[]) source.readParcelableArray(null);
    if (binary != null) {
      this.binaryData = new ArrayList<BinaryData<?>>(Arrays.asList(binary));
    }
  }

  @Override
  public void writeToParcel(final Parcel dest, final int flags) {
    dest.writeInt(id);
    dest.writeInt(operationType);
    dest.writeString(url);
    dest.writeString(cacheName);
    dest.writeParcelable(simpleParameters, flags);
    dest.writeString(contentType);
    dest.writeString(encoding.name());
    dest.writeString(contentLanguage);
    dest.writeParcelable(metaParameters, flags);
    dest.writeInt(parallelMode ? 1 : 0);
    dest.writeInt(canceled ? 1 : 0);

    dest.writeParcelable(modelType, 0);
    dest.writeString(contentHandler);
    dest.writeString(contentAnalyzer);

    // binary content fields
    if (binaryData != null) {
      final BinaryData<?>[] binary = new BinaryData<?>[binaryData.size()];
      dest.writeParcelableArray(binaryData.toArray(binary), flags);
    } else {
      dest.writeParcelableArray(null, flags);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof RequestDescription)) { return false; }
    return id == ((RequestDescription)o).getId();
  }

  @Override
  public int hashCode() { return id; }

  @Override
  public final int describeContents() {
    final ArrayList<BinaryData<?>> binaryData = this.binaryData;
    if (binaryData == null) { return 0; }
    final int count = binaryData.size();
    int result = 0;
    for (int i = 0; i < count; i++) {
      result |= binaryData.get(i).describeContents();
    }
    return result;
  }

  /** @return request identifier */
  public int getId() { return id; }

  /** @param operationType operation type */
  public void setOperationType(final int operationType) { this.operationType = operationType; }
  /** @return the operationType */
  public int getOperationType() { return operationType; }

  /** @return the contentLanguage */
  public String getContentLanguage() { return contentLanguage; }
  /** @param contentLanguage the contentLanguage to set */
  public void setContentLanguage(final String contentLanguage) { this.contentLanguage = contentLanguage; }
  /** @return the contentType */
  public String getContentType() { return contentType; }
  /** @param contentType the contentType to set */
  public void setContentType(final String contentType) { this.contentType = contentType; }
  /** @return URL */
  public String getUrl() { return url; }
  /** @param url URL to set */
  public void setUrl(final String url) { this.url = url; }
  /** @return cache manager name */
  public String getCacheName() { return cacheName; }
  /** @param cacheName cache manager name */
  public void setCacheName(final String cacheName) { this.cacheName = cacheName; }
  /** @param encoding request encoding */
  public void setEncoding(final Charset encoding) { this.encoding = encoding; }
  /** @return request encoding */
  public Charset getEncoding() { return encoding; }

  /** @param typeToken type token the response model */
  public void setModelType(final ModelTypeToken typeToken) { this.modelType = typeToken; }
  /** @return type token the response model */
  public ModelTypeToken getModelType() { return modelType; }
  /** @param contentHandler content handler name */
  public void setContentHandler(final String contentHandler) { this.contentHandler = contentHandler; }
  /** @return content handler name */
  public String getContentHandler() { return contentHandler; }

  public void setContentAnalyzer(final String contentAnalyzer) { this.contentAnalyzer = contentAnalyzer; }
  public String getContentAnalyzer() { return contentAnalyzer; }

  public void setCanceled(final boolean canceled) { this.canceled = canceled; }
  public boolean isCanceled() { return canceled; }

  public ArrayList<BinaryData<?>> getBinaryData() { return binaryData; }
  public void addBinaryData(final BinaryData<?> bdata) {
    if (binaryData == null) { binaryData = new ArrayList<BinaryData<?>>(); }
    binaryData.add(bdata);
  }
  public void clearBinaryData() {
    if (binaryData != null) {
      binaryData.clear();
    }
  }


  /** @return whether request is simple. */
  public boolean isSimple() { return operationType == OperationType.SIMPLE_POST || operationType == OperationType.SIMPLE_GET; }

  /** @param parallelMode parallel mode flag */
  public void setParallelMode(final boolean parallelMode) { this.parallelMode = parallelMode; }
  /** @return parallel mode flag */
  public boolean isParallelMode() { return parallelMode; }

  /** @return the metaParameters */
  public ParametersGroup getMetaParameters() { return metaParameters; }

  /** @return the simpleParameters */
  public ParametersGroup getSimpleParameters() { return simpleParameters; }

  public ParametersGroup createMetaParameters() {
    metaParameters = new ParametersGroup();
    metaParameters.name = "meta";
    return metaParameters;
  }

  /**
   * @param name parameter name
   * @param value parameter value
   */
  public void addMetaInfo(final String name, final String value) {
    ParametersGroup metaParameters = this.metaParameters;
    if (metaParameters == null) {
      metaParameters = createMetaParameters();
    }

    final ParameterValue pv = new ParameterValue();
    pv.name = name;
    pv.value = value;
    metaParameters.children.add(pv);
  }

  public String getMetaInfo(final String name) {
    final ParametersGroup metaParameters = this.metaParameters;
    if (metaParameters == null) { return null; }
    for (final Parameter p : metaParameters.children) {
      if (name.equals(p.name)) { return ((ParameterValue)p).value; }
    }
    return null;
  }

  // ============================ HTTP REQUESTS ============================

  /**
   * A good place to set custom request headers.
   * @param urlConnection URL connection instance
   */
  protected void onURLConnectionPrepared(final URLConnection urlConnection) {
    if (contentType != null) {
      urlConnection.addRequestProperty("Content-Type", contentType);
    }
    if (contentLanguage != null) {
      urlConnection.addRequestProperty("Accept-Language", contentLanguage);
    }
    urlConnection.addRequestProperty("Accept-Encoding", IoUtils.ENCODING_GZIP);
  }

  /**
   * Build {@link URLConnection} instance, connect, write request.
   * @param context context instance
   * @return {@link URLConnection} instance, ready for {@link URLConnection#getInputStream()} call
   * @throws IOException in case of I/O errors
   */
  public URLConnection makeConnection(final Context context) throws IOException {
    final BaseRequestDescriptionConverter converter = CONVERTERS.get(operationType);

    final URLConnection connection = converter.prepareConnectionInstance(context, this);
    onURLConnectionPrepared(connection);
    connection.connect();
    converter.sendRequest(context, connection, this);

    return connection;
  }

}