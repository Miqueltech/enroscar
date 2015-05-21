**Contents**


Our task here is to write a basic 'hello world' with Enroscar.
But not only should the greeting text be displayed on the screen but also let's show android developers logo loaded from the web site:

![http://developer.android.com/assets/images/bg_logo.png](http://developer.android.com/assets/images/bg_logo.png)


# Application #
First of all create your application class that extends Application provided by this library.
In `onCreate()` method define your application authority. It can be your package name in the simplest case. This authority will be used by remote images manager to access information about cached images.
```
package com.stanfy.enroscar.sample;

import com.stanfy.app.Application;

/**
 * Sample application.
 */
public class SampleApplication extends Application {

  /** Application authority for content provider configuration. */
  public static final String APP_AUTHORITY = "com.stanfy.enroscar.sample";

  @Override
  public void onCreate() {
    super.onCreate();
    setImagesDAOAuthority(APP_AUTHORITY);
  }

}
```
Pay attention to `APP_AUTHORITY` value. It will be used in manifest file.

# First Activity and Fragment #
Now let's create our first activity and fragment. Sure, task of displaying remote image can be solved without fragments.
But we strongly insist on using fragments since they are very powerful building block of your application.

## Fragment ##
Start with a fragment. Describe its layout in an XML file and include LoadableImageView that will display the remote image.
```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    style="@style/MainContainer"
    >

  <TextView
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:text="@string/hello_fragment" 
      />
  
  <com.stanfy.views.LoadableImageView
      android:layout_width="180dp"
      android:layout_height="50dp"
      android:id="@+id/logo"
      />
  
</LinearLayout>
```

Now it's time to write the code of our fragment (`DevelopersLogoFragment` class) extending base fragment class provided by the library.
This fragment creates its view inflating it from XML resource. Also it configures an image view that was described in layout to use
special images manager context (see `logo.setImagesManagerContext(...)` line) for treating remote images and specifies remote image location
with an appropriate URL (see `logo.setImageURI(...)`).
```
package com.stanfy.enroscar.sample;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.stanfy.app.BaseFragment;
import com.stanfy.views.LoadableImageView;

/**
 * Sample fragment.
 */
public class DevelopersLogoFragment extends BaseFragment<SampleApplication> {

  @Override
  public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.main, container, false);
    final LoadableImageView logo = (LoadableImageView)view.findViewById(R.id.logo);
    logo.setImagesManagerContext(getOwnerActivity().getApp().getImagesContext());
    logo.setImageURI(Uri.parse("http://developer.android.com/assets/images/bg_logo.png"));
    return view;
  }

}
```

## Activity ##
Now we should create an activity that will contain our fragment. Let's start with its layout like we did it with the fragment.
```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    style="@style/MainContainer"
    android:id="@id/fragment_container"
    >

  <TextView
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:text="@string/hello" 
      />
  
</LinearLayout>
```
Our activity is supposed to add the fragment view under 'hello' message. We give `@id/fragment_container` ID to the `LinearLayout` indicating
a view that will contain the fragment view.

Here's activity code:
```
package com.stanfy.enroscar.sample;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.stanfy.app.activities.OneFragmentActivity;

public class SampleActivity extends OneFragmentActivity<SampleApplication> {

  @Override
  protected Fragment createFragment(final Bundle savedInstanceState) {
    return new DevelopersLogoFragment();
  }

  @Override
  protected int getLayoutId() { return R.layout.activity; }

}
```
As you see our activity extends `OneFragmentActivity` class, which means that in its `onCreate()` callback this activity inflates layout
from the resource identified by the return value of `getLayoutId()` method and commits a fragment transaction that adds a fragment
created in `createFragment(Bundle)` method. Fragment view is added to a view that has `@id/fragment_container` identifier.

# Manifest File #
Now let's edit our AndroidManifest.xml in order to specify what activity to start from launcher and configure
other essential application components.
```
<?xml version="1.0" encoding="utf-8"?>
<manifest 
    xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.stanfy.enroscar.sample"
    android:versionCode="1"
    android:versionName="1.0" 
    >

  <uses-sdk 
      android:minSdkVersion="7" 
      android:targetSdkVersion="10" 
      />
  
  <!-- It's obvious. -->
  <uses-permission android:name="android.permission.INTERNET" />
  <!-- We try to store cached images on SD card. -->
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />  
  
  <!-- Application description -->
  <application
      android:name=".SampleApplication"
      android:icon="@drawable/ic_launcher"
      android:label="@string/app_name"
      android:theme="@android:style/Theme.Light" 
      >
    
    <!-- Our first activity. -->
    <activity
        android:name=".SampleActivity"
        >
      <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
    </activity>
    
    <!-- Main application service. Deals with remote server API. -->
    <service android:name="com.stanfy.app.service.ApplicationService" />
    
    <!-- Application content provider -->
    <!-- Look at android:authorities="com.stanfy.enroscar.sample". 
         It should be equal to APP_AUTHORITY value which you pass to setImagesDAOAuthority in the application class.  -->
    <provider
        android:name="com.stanfy.content.DefaultContentProvider"
        android:authorities="com.stanfy.enroscar.sample"
        />
      
  </application>

</manifest>
```

Now, we hope, you can launch your application and see Android developers logo on the screen.

# Communicating with remote server #
See our sample project that demonstrates how to fetch data from remote servers. This sample displays a list of last tweets
from @twitterapi.
To implement similar functions you'll have to create your own class that extends `RequestMethodHelper` and can provide appropriate
parser contexts. This helper must be created in `createRequestMethodHelper()` method of your application class (see SampleApplication).
Try to run our sample project and click 'Show tweets' button.

## Enumerate your operations ##
Enroscar library uses `Operation` interface to identify all the API operations.
```
/**
 * Enumerate your operations here.
 */
public enum OurOperation implements Operation {

  GET_TWEETS(SIMPLE_GET, "https://api.twitter.com/1/statuses/user_timeline.json");

  /** Type */
  private final int type;
  /** URL. */
  private String url;

  private OurOperation(final int type, final String url) {
    this.type = type;
    this.url = url;
  }

  @Override
  public int getCode() { return ordinal(); }
  @Override
  public int getType() { return type; }
  @Override
  public String getUrlPart() { return url; }

  public static OurOperation byCode(final int code) { return OurOperation.values()[code]; }

}
```
Here we described an operation for fetching list of tweets. If you add another remote server API operation it should be add here, e.g:
```
  GET_TWEETS(SIMPLE_GET, "https://api.twitter.com/1/statuses/user_timeline.json"),
  POST_TWEET(SIMPLE_POST, "http://api.twitter.com/1/statuses/update.json");
```

## Create parser context ##


## Create a request builder class ##
In order to provide parameters to your operation and execute it create a request builder class.
```
/**
 * Request builder for {@link OurOperation#GET_TWEETS} operation.
 */
public class TweetsRequestBuilder extends ListRequestBuilder {

  public TweetsRequestBuilder(final Context context) {
    super(context);
  }

  @Override
  public Operation getOperation() { return OurOperation.GET_TWEETS; }

  public TweetsRequestBuilder setScreenname(final String name) {
    addSimpleParameter("screen_name", name);
    return this;
  }

  @Override
  public ListRequestBuilder setOffset(final int offset) {
    return super.setOffset(offset + 1);
  }

  @Override
  public String getOffsetParamName() { return "page"; }

}
```

## Use the request builder ##
After request builder class is created you can use its instance to run your API requests. To create an instance of request builder you'll need a request executor - object that knows how to communicate with the applicatio service and ask it to perform a request. Request executor can be obtained with `getRequestExecutor()` call from your activity that extends `BaseActivity`. Use `getOwnerActivity().getRequestExecutor()` from your fragments.
```
  // call from a fragment
  new TweetsRequestBuilder (getOwnerActivity(), getOwnerActivity().getRequestExecutor())
    .setScreenname("twitterapi")
    .setOffset(2)
    .execute();
```

## Create parser context ##
```
  /**
   * Factory for request descriptions and parser contexts.
   */
  private static class RequestMethodProvider extends RequestMethodHelper {

    public RequestMethodProvider() {
      super(TYPE_JSON, SampleApplication.APP_AUTHORITY);
    }

    @Override
    public ParserContext createParserContext(final RequestDescription requestDescription) {
      switch (OurOperation.byCode(requestDescription.getOperationCode())) {
      case GET_TWEETS:
        return OneClassModelParserContext.create(new TypeToken<ArrayList<Tweet>>() {});
      default:
        return super.createParserContext(requestDescription);
      }
    }

  }
```