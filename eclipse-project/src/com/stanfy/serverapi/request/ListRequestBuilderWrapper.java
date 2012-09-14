package com.stanfy.serverapi.request;



import java.util.List;

import android.content.Context;

import com.stanfy.app.loader.LoadMoreListLoader;
import com.stanfy.serverapi.response.ModelTypeToken;
import com.stanfy.utils.RequestExecutor;

/**
 * Base request builder to get a list of entities.
 * @author Roman Mazur - Stanfy (http://www.stanfy.com)
 * @param <MT> model type
 * @param <LT> list type
 */
public abstract class ListRequestBuilderWrapper<LT extends List<MT>, MT> implements ListRequestBuilder<LT, MT> {

  /** Standard `offset` parameter name. */
  public static final String PARAM_OFFSET = "offset";
  /** Standard `limit` parameter name. */
  public static final String PARAM_LIMIT = "limit";

  /** Core builder. */
  private final BaseRequestBuilder<LT> core;

  /** Expected type token. */
  private final ModelTypeToken expectedTypeToken;

  /** Parameter name. */
  private String offsetName = PARAM_OFFSET, limitName = PARAM_LIMIT;

  /** Offset value. */
  private ParameterValue offsetValue;
  /** Offset value. */
  private ParameterValue limitValue;

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public ListRequestBuilderWrapper(final BaseRequestBuilder core) {
    this.core = core;
    this.expectedTypeToken = ModelTypeToken.fromRequestBuilderClass(getClass());
  }

  @Override
  public ListRequestBuilderWrapper<LT, MT> setLimit(final String limit) {
    if (limitValue == null) {
      limitValue = core.addSimpleParameter(limitName, limit);
    } else {
      limitValue.setValue(limit);
    }
    return this;
  }

  @Override
  public ListRequestBuilderWrapper<LT, MT> setOffset(final String offset) {
    if (offsetValue == null) {
      offsetValue = core.addSimpleParameter(offsetName, offset);
    } else {
      offsetValue.setValue(offset);
    }
    return this;
  }

  public ListRequestBuilderWrapper<LT, MT> setOffset(final int offset) {
    return setOffset(String.valueOf(offset));
  }

  public ListRequestBuilderWrapper<LT, MT> setLimit(final int offset) {
    return setLimit(String.valueOf(offset));
  }

  /** @param offsetName `offset` parameter name */
  public void setOffsetParamName(final String offsetName) {
    this.offsetName = offsetName;
  }
  /** @param limitName `limit` parameter name */
  public void setLimitParamName(final String limitName) {
    this.limitName = limitName;
  }

  @Override
  public Context getContext() { return core.getContext(); }

  @Override
  public int execute() { return core.execute(); }

  @Override
  public void setExecutor(final RequestExecutor executor) { core.setExecutor(executor); }

  @Override
  public String getOffset() { return offsetValue != null ? offsetValue.getValue() : "0"; }

  @Override
  public String getLimit() { return limitValue != null ? limitValue.getValue() : "0"; }

  @Override
  public LoadMoreListLoader<MT, LT> getLoader() {
    return new LoadMoreListLoader<MT, LT>(this);
  }

  @Override
  public ModelTypeToken getExpectedModelType() { return expectedTypeToken; }

  @Override
  public String toString() {
    return "wrapper-" + getClass().getName() + "(" + core + ")";
  }

}
