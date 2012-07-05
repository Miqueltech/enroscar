package com.stanfy.app.loader;

import java.util.List;

import android.util.Log;

import com.stanfy.serverapi.request.ListRequestBuilder;
import com.stanfy.serverapi.response.ResponseData;

/**
 * Request builder loader that can load more data.
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 *
 * @param <MT> model type
 * @param <LT> list type
 */
public class LoadMoreListLoader<MT, LT extends List<MT>> extends RequestBuilderLoader<LT> implements LoadmoreLoader {

  /** Incremenetor. */
  private ValueIncrementor offsetIncrementor, limitIncrementor;

  /** Items list. */
  private LT itemsList;

  /** Current offset. */
  private int offset, limit;

  /** Last loaded count. */
  private int lastLoadedCount;

  /** Stop load more state indicator. */
  private boolean stopLoadMore;

  public LoadMoreListLoader(final ListRequestBuilder<LT, MT> requestBuilder) {
    super(requestBuilder);
    this.offset = requestBuilder.getOffset();
    this.limit = requestBuilder.getLimit();
  }

  @SuppressWarnings("unchecked")
  @Override
  public ListRequestBuilder<LT, MT> getRequestBuilder() { return (ListRequestBuilder<LT, MT>) super.getRequestBuilder(); }

  public RequestBuilderLoader<LT> setOffsetIncrementor(final ValueIncrementor incrementor) {
    this.offsetIncrementor = incrementor;
    return this;
  }
  public RequestBuilderLoader<LT> setLimitIncrementor(final ValueIncrementor incrementor) {
    this.limitIncrementor = incrementor;
    return this;
  }

  protected final int nextOffset() {
    if (offsetIncrementor == null) { return offset + 1; }
    return offsetIncrementor.nextValue(offset, lastLoadedCount, itemsList.size());
  }
  protected final int nextLimit() {
    if (offsetIncrementor == null) { return limit; }
    return limitIncrementor.nextValue(limit, lastLoadedCount, itemsList.size());
  }

  @Override
  protected ResponseData<LT> onAcceptData(final ResponseData<LT> oldData, final ResponseData<LT> data) {
    final LT list = data.getModel();
    if (list == null) {
      // error case
      stopLoadMore = true;
      return data;
    }

    lastLoadedCount = list.size();
    if (lastLoadedCount == 0) {
      stopLoadMore = true;
    } else {
      if (itemsList != null) {
        list.addAll(0, itemsList);
      }
      itemsList = list;
    }
    return data;
  }

  @Override
  public void forceLoadMore() {
    final int nOffset = nextOffset();
    if (nOffset < 0) {
      if (DEBUG) { Log.d(TAG, "Negative offset, cancel loadmore"); }
      stopLoadMore = true;
      return;
    }

    offset = nOffset;
    limit = nextLimit();

    getRequestBuilder().setOffset(offset).setLimit(limit);
    forceLoad();
  }

  @Override
  public boolean moreElementsAvailable() {
    // TODO implement load more offset tracking
    return !stopLoadMore;
  }

  /**
   * Offset/limit incrementor.
   */
  public interface ValueIncrementor {
    int nextValue(int currentValue, int lastLoadedCount, int currentCount);
  }

}
