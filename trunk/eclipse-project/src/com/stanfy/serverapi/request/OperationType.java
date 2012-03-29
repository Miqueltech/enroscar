package com.stanfy.serverapi.request;

/**
 * Possible operation types.
 * @author Roman Mazur (mailto: mazur.roman@gmail.com)
 */
public class OperationType {

  /** Simple post. */
  public static final int SIMPLE_POST = 0;

  /** Upload post. Multi-part should be used for HTTP. */
  public static final int UPLOAD_POST = 1;

  /** Simple get. */
  public static final int SIMPLE_GET = 2;

  protected OperationType() { /* hide and allow extending */ }

}
