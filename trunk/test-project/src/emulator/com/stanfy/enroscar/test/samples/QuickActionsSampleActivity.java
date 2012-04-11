package com.stanfy.enroscar.test.samples;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.stanfy.enroscar.test.R;
import com.stanfy.views.qa.BasicQuickAction;
import com.stanfy.views.qa.BasicQuickActionsAdapter;
import com.stanfy.views.qa.QuickActionsBar;
import com.stanfy.views.qa.QuickActionsWidget;
import com.stanfy.views.qa.QuickActionsWidget.OnQuickActionClickListener;

/**
 * Sample for quick actions.
 * @author Roman Mazur (Stanfy - http://www.stanfy.com)
 */
public class QuickActionsSampleActivity extends Activity {

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.qa);
    final BasicQuickActionsAdapter adapter = new BasicQuickActionsAdapter(this,
        new BasicQuickAction(this, R.drawable.icon, R.string.app_name),
        new BasicQuickAction(this, R.drawable.icon, R.string.app_name)
    );
    final QuickActionsBar qaBar = new QuickActionsBar(this);
    qaBar.setQuickActionsAdapter(adapter);
    findViewById(R.id.qa_button).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(final View v) {
        qaBar.show(v);
      }
    });
    qaBar.setOnQuickActionClickListener(new OnQuickActionClickListener() {
      @Override
      public void onQuickActionClicked(final QuickActionsWidget widget, final int position) {
        Log.d("123123", "Pos " + position);
      }
    });
  }

}
