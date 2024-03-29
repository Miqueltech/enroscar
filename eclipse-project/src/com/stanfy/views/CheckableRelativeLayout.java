package com.stanfy.views;

import static com.stanfy.views.CheckableConsts.CHECKED_ATTRS;
import static com.stanfy.views.CheckableConsts.CHECKED_STATE_SET;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Checkable;
import android.widget.RelativeLayout;

/**
 * {@link RelativeLayout} that implements {@link Checkable}.
 * @see RelativeLayout
 */
public class CheckableRelativeLayout extends RelativeLayout implements Checkable {

  /** On checked change listener. */
  private OnCheckedChangeListener listener;

  /** Checked flag. */
  private boolean checked;

  /**
   * @param context
   */
  public CheckableRelativeLayout(final Context context) {
    super(context);
  }

  /**
   * @param context
   * @param attrs
   * @param defStyle
   */
  public CheckableRelativeLayout(final Context context, final AttributeSet attrs, final int defStyle) {
    super(context, attrs, defStyle);
    init(context, attrs);
  }

  /**
   * @param context
   * @param attrs
   */
  public CheckableRelativeLayout(final Context context, final AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  private void init(final Context context, final AttributeSet attrs) {
    final TypedArray a = context.obtainStyledAttributes(attrs, CHECKED_ATTRS);
    setChecked(a.getBoolean(0, false));
    a.recycle();
  }

  /**
   * <p>Changes the checked state of this text view.</p>
   *
   * @param checked true to check the text, false to uncheck it
   */
  @Override
  public void setChecked(final boolean checked) {
    if (this.checked != checked) {
      this.checked = checked;
      refreshDrawableState();
      if (listener != null) { listener.onCheckedChanged(checked); }
    }
  }

  @Override
  public void toggle() {
    setChecked(!checked);
  }

  @Override
  public boolean isChecked() {
    return checked;
  }

  @Override
  public boolean dispatchPopulateAccessibilityEvent(final AccessibilityEvent event) {
      boolean populated = super.dispatchPopulateAccessibilityEvent(event);
      if (!populated) {
          event.setChecked(checked);
      }
      return populated;
  }

  @Override
  protected int[] onCreateDrawableState(final int extraSpace) {
    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
    if (isChecked()) {
      mergeDrawableStates(drawableState, CHECKED_STATE_SET);
    }
    return drawableState;
  }

  /** @return current on checked change listener instance */
  public OnCheckedChangeListener getOnCheckedChangeListener() { return listener; }
  /** @param listener on checked change listener to set */
  public void setOnCheckedChangeListener(final OnCheckedChangeListener listener) { this.listener = listener; }

  /**
   * <p>
   * Interface definition for a callback to be invoked when the checked state
   * is changed.
   * </p>
   */
  public interface OnCheckedChangeListener {
    /**
     * <p>
     * Called when the checked state has changed.
     * </p>
     *
     * @param value
     *          checked value
     */
    void onCheckedChanged(final boolean value);
  }

}
