package com.stanfy.app.fragments;

import android.os.Bundle;

import com.stanfy.app.BaseFragment;
import com.stanfy.utils.OneRequestModelBehavior;

/**
 * Fragment that contains a request builder instance and is oriented on processing
 * a particular model type.
 * @param <MT> model type
 * @author Roman Mazur (Stanfy - http://www.stanfy.com)
 */
public abstract class OneRequestModelFragment<MT> extends BaseFragment implements OneRequestModelBehavior<MT> {

  /** Core object. */
  private final OneRequestModelHelper<MT> core = new OneRequestModelHelper<MT>(this);

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    core.fetch();
  }

}