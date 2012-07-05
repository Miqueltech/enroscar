package com.stanfy.views;

import android.view.View;

import com.stanfy.app.CrucialGUIOperationManager;
import com.stanfy.app.beans.BeansManager;

/**
 * @author Roman Mazur (Stanfy - http://stanfy.com)
 */
public final class AnimatedViewHelper {

  /** Application instance. */
  private final CrucialGUIOperationManager manager;

  /** Owner view. */
  private final View owner;

  /** Worker that notifies about finished operation. */
  private final Runnable crucialGuiOperationFinishedWorker = new Runnable() {
    @Override
    public void run() {
      manager.dispatchCrucialGUIOperationFinish();
    }
  };

  public AnimatedViewHelper(final View owner) {
    this.owner = owner;
    this.manager = BeansManager.get(owner.getContext()).getCrucialGUIOperationManager();
  }

  public void notifyCrucialGuiStart() {
    owner.removeCallbacks(crucialGuiOperationFinishedWorker);
    manager.dispatchCrucialGUIOperationStart();
  }

  public void notifyCrucialGuiFinish() {
    final int delay = 500;
    owner.postDelayed(crucialGuiOperationFinishedWorker, delay);
  }

  public void onDetach() {
    owner.removeCallbacks(crucialGuiOperationFinishedWorker);
    manager.dispatchCrucialGUIOperationFinish();
  }

}
