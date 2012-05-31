package com.stanfy.serverapi.request;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.stanfy.DebugFlags;
import com.stanfy.http.multipart.MultipartEntity;
import com.stanfy.http.multipart.Part;
import com.stanfy.http.multipart.StringPart;
import com.stanfy.serverapi.request.binary.BinaryData;

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

  /** Request ID. */
  final int id;

  /** Token. It can be used to identify a sender. */
  int token;
  /** Operation to execute. */
  int operationCode;

  /** Operation type. */
  int operationType;

  /** URL part. */
  String urlPart;
  /** Simple parameters. */
  ParametersGroup simpleParameters;

  /** Content type. */
  String contentType;
  /** Content language. */
  String contentLanguage;

  /** Meta information. */
  ParametersGroup metaParameters;

  /** Binary data array. */
  ArrayList<BinaryData<?>> binaryData;

  /** Whether request should be performed in parallel. */
  boolean parallelMode = false;

  public static String getParamValue(final String name, final LinkedList<Parameter> param) {
    for (final Parameter p : param) {
      if (p instanceof ParameterValue && name.equals(p.getName())) {
        return ((ParameterValue)p).getValue();
      }
    }
    return null;
  }

  public RequestDescription() {
    this.id = nextId();
  }

  /**
   * Create from parcel.
   */
  protected RequestDescription(final Parcel source) {
    this.id = source.readInt();
    this.token = source.readInt();
    this.operationCode = source.readInt();
    this.operationType = source.readInt();
    this.urlPart = source.readString();
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    this.simpleParameters = source.readParcelable(cl);
    this.contentType = source.readString();
    this.contentLanguage = source.readString();
    this.metaParameters = source.readParcelable(cl);
    this.parallelMode = source.readInt() == 1;

    // binary content fields
  }

  @Override
  public void writeToParcel(final Parcel dest, final int flags) {
    dest.writeInt(id);
    dest.writeInt(token);
    dest.writeInt(operationCode);
    dest.writeInt(operationType);
    dest.writeString(urlPart);
    dest.writeParcelable(simpleParameters, flags);
    dest.writeString(contentType);
    dest.writeString(contentLanguage);
    dest.writeParcelable(metaParameters, flags);
    dest.writeInt(parallelMode ? 1 : 0);

    // binary content fields
  }

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

  void setupOperation(final Operation op) {
    this.operationCode = op.getCode();
    this.operationType = op.getType();
    this.urlPart = op.getUrlPart();
    if (DEBUG) { Log.v(TAG, "Setup request operation OPCODE: " + operationCode + " OPTYPE: " + operationType + " URL: " + urlPart); }
  }

  /** @return request identifier */
  public int getId() { return id; }

  /** @return the operationType */
  public int getOperationType() { return operationType; }

  /** @return the operation */
  public int getOperationCode() { return operationCode; }
  /** @return the token */
  public int getToken() { return token; }

  /** @return the contentLanguage */
  public String getContentLanguage() { return contentLanguage; }
  /** @param contentLanguage the contentLanguage to set */
  public void setContentLanguage(final String contentLanguage) { this.contentLanguage = contentLanguage; }
  /** @return the contentType */
  public String getContentType() { return contentType; }
  /** @param contentType the contentType to set */
  public void setContentType(final String contentType) { this.contentType = contentType; }
  /** @return the urlPart */
  public String getUrlPart() { return urlPart; }
  /** @param urlPart the urlPart to set */
  public void setUrlPart(final String urlPart) { this.urlPart = urlPart; }

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

  protected String resolveSimpleGetRequest(final Context context) {
    final Uri.Builder builder = Uri.parse(urlPart).buildUpon();
    for (final Parameter p : this.simpleParameters.children) {
      if (p instanceof ParameterValue) {
        builder.appendQueryParameter(p.getName(), ((ParameterValue) p).getValue());
      }
    }
    final String result = builder.build().toString();
    if (DEBUG) { Log.d(TAG, "(" + id + ")" + ": " + result); }
    return result;
  }

  protected void resolveSimpleEntityRequest(final HttpRequestBase request, final Context context) throws UnsupportedEncodingException {
    final LinkedList<BasicNameValuePair> parameters = new LinkedList<BasicNameValuePair>();
    for (final Parameter p : this.simpleParameters.children) {
      if (p instanceof ParameterValue) {
        parameters.add(new BasicNameValuePair(p.name, ((ParameterValue)p).value));
      }
    }
    if (request instanceof HttpEntityEnclosingRequestBase) {
      ((HttpEntityEnclosingRequestBase)request).setEntity(new UrlEncodedFormEntity(parameters, CHARSET));
    }
    if (DEBUG) { Log.d(TAG, "(" + id + ")" + ": " + parameters.toString()); }
  }

  protected void resolveMultipartRequest(final HttpPost request, final Context context) throws IOException {
    final List<Parameter> params = simpleParameters.children;
    int realCount = 0;
    final int binaryCount = binaryData != null ? binaryData.size() : 0;
    Part[] parts = new Part[params.size() + binaryCount];
    for (final Parameter p : params) {
      if (p instanceof ParameterValue) {
        final ParameterValue pv = (ParameterValue)p;
        if (pv.value == null) { continue; }
        parts[realCount++] = new StringPart(pv.name, pv.value, CHARSET);
      }
    }
    for (int i = 0; i < binaryCount; i++) {
      final Part part = binaryData.get(i).createHttpPart(context);
      if (part != null) {
        parts[realCount++] = part;
      }
    }
    if (realCount < parts.length) {
      final Part[] trim = new Part[realCount];
      System.arraycopy(parts, 0, trim, 0, realCount);
      parts = trim;
    }
    request.setEntity(new MultipartEntity(parts));
    if (DEBUG) { Log.d(TAG, "(" + id + ")" + ": " + params); }
  }


  /**
   * @param context context instance
   * @return HTTP request instance
   */
  public HttpUriRequest buildRequest(final Context context) {
    final HttpRequestBase result;

    try {
      switch (operationType) {
      case OperationType.UPLOAD_POST:
        result = new HttpPost(urlPart);
        resolveMultipartRequest((HttpPost)result, context);
        break;
      case OperationType.SIMPLE_GET:
        result = new HttpGet(resolveSimpleGetRequest(context));
        break;
      case OperationType.SIMPLE_POST:
        result = new HttpPost(urlPart);
        resolveSimpleEntityRequest(result, context);
        break;
      default:
        throw new IllegalArgumentException("Bad operation type for code " + operationCode + ", type " + operationType);
      }
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

}
