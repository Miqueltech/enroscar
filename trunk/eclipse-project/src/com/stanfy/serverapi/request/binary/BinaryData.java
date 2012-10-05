package com.stanfy.serverapi.request.binary;

import java.io.IOException;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.stanfy.serverapi.request.RequestDescription;
import com.stanfy.serverapi.request.net.multipart.Part;

/**
 * Binary data info.
 * @param <T> binary data
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 */
public abstract class BinaryData<T extends Parcelable> implements Parcelable {

  /** Logging tag. */
  protected static final String TAG = RequestDescription.TAG;

  /** Binary data name. */
  private String name;
  /** Binary data content name. */
  private String contentName;
  /** Binary data. */
  private T data;
  /** Content type. */
  private String contentType;

  public BinaryData() {
    // nothing
  }

  public BinaryData(final Parcel source) {
    this.name = source.readString();
    this.contentName = source.readString();
    this.data = source.readParcelable(getClass().getClassLoader());
  }

  @Override
  public int describeContents() { return data != null ? data.describeContents() : 0; }

  @Override
  public void writeToParcel(final Parcel dest, final int flags) {
    dest.writeString(name);
    dest.writeString(contentName);
    dest.writeParcelable(data, flags);
  }

  /** @param binaryDataName string used to name binary data */
  public void setName(final String binaryDataName) { this.name = binaryDataName; }
  /** @return string used to name binary data */
  public String getName() {
    return TextUtils.isEmpty(name) ? RequestDescription.BINARY_NAME_DEFAULT : name;
  }

  public void setContentName(final String contentName) { this.contentName = contentName; }
  /** @return file name of the binary data */
  public String getContentName() { return contentName; }

  /**
   * @param contentType content type value
   */
  public void setContentType(final String contentType) {
    this.contentType = contentType;
  }
  /**
   * @return content type
   */
  public String getContentType() {
    return contentType;
  }

  protected void setData(final T data) { this.data = data; }
  public T getData() { return data; }

  public void clear() {
    this.data = null;
    this.name = null;
    this.contentName = null;
  }

  /** @return part instance */
  public abstract Part createHttpPart(final Context context) throws IOException;

}
