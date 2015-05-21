

# Server API Requests #

The main idea is that API requests processing is performed by the service. It’s a bound service and it can provide an interface for initiating API requests to the server side and listening to their results.
You will find a lot of interesting thoughts about such an approach in [a rather interesting Google IO session about developing Android REST client application](http://www.google.com/events/io/2010/sessions/developing-RESTful-android-apps.html).

Activities and fragments bind to this service and register their callbacks in order to listen to requests results. Service binding is performed when activity starts, and unbinding - when activity stops.

Details can be found in ["Server API requests invocation"](ServerAPIRequestsInvocation.md) article.

# Images Manager #

Images manager provided by the library solves the problem of displaying images that are loaded from remote servers over HTTP. It uses 2-level cache: memory and local file system (SD card).

Usage is rather easy but requires content provider configuration. Images can be displayed by one of "Loadable" views: LoadableImageView, LoadableTextView, LoadableCompoundButton. You may also implement custom handling of drawables provided by images manager with ImageHolder.

# Custom Views #

Here is a list of custom views provided by the library and short notes about their mission. See also [Custom Views Description](CustomViews.md) for more details.

| **View** | **Notes** |
|:---------|:----------|
|ImageView |Extends standard [ImageView](http://developer.android.com/intl/de/reference/android/widget/ImageView.html) and allows you to modify displayed images with decorators located in package com.stanfy.images.decorator|
|EllipsizingTextView|A workaround for Android [TextView](http://developer.android.com/intl/de/reference/android/widget/TextView.html) problem with ellipsizing multiline texts http://code.google.com/p/android/issues/detail?id=2254. Thanks to its authors!|
|HorizontalScrollView, ScrollView|Extend standard scroll views and can maintain their scroll position in case of recreation (e.g. after screen orientation change). Besides these two views can substitute each other in different layouts sharing the same identifier.|
| GalleryView | Rewritten standard Android [GalleryView](http://developer.android.com/reference/android/widget/Gallery.html) in order to recycle children views. It also can notify application about its animations (scrolling) for preventing other hard operations (e.g. pausing images manager).  |
| ListView | Extends standard Android [ListView](http://developer.android.com/reference/android/widget/ListView.html) and and can maintain their scroll position in case of recreation (e.g. after screen orientation change). It also can notify application about its animations (scrolling) for preventing other hard operations (e.g. pausing images manager).  |
| FetchableListView | A list view that can ask its adapter to load more elements when user scrolls to the bottom edge |

# Common Activities Behavior #

Android framework provides a set of activity classes, which means that extending them you'll create different subtrees in your classes hierarchy. This makes a bit difficult to incapsulate your common activities logic. That's why your application activity classes should extend "Base" activities from the library. Then you'll be able to implement common logic in BaseActivityBehavior that is created by Application object.

# Statistics Tools Integration #

Statistics tools integration is done with StatsManager interface. Base activity behavior calls `onStartScreen()` on first start of an activity, `onComeToScreen()` on every start of an activity, and `onLeaveScreen()` on every stop of an activity.
This library also provides integration with [Flurry](http://www.flurry.com/) (see FlurryStatsManager). It can be configured with overriding `createStatsManager()` method of your application class.
```
...
@Override
protected StatsManager createStatsManager() {
  return new FlurryStatsManager(YOUR_KEY);
}
@Override
public StatsManagerAdapter getStatsManager() {  
  // FlurryStatsManager extends StatsManagerAdapter which provides more sweet methods
  return (StatsManagerAdapter)super.getStatsManager();
}
...
```

# Download Service #
This service can be used instead of [DownloadManager API](http://developer.android.com/reference/android/app/DownloadManager.html) that is available since level 9 (Android 2.3). To start downloading create a request object and send it within the instent starting this service.
```
long id = DownloadsService.nextId(context);
final Request request = new Request();
request.setId(id);
request.setUri(DOWNLOAD_URI);
request.setTitle(DOWNLOAD_TITLE);
request.setDescription(DOWNLOAD_DESCRIPTION);
request.setDestinationUri(DESTINATION_FILE_URI);
final Intent requestIntent = new Intent(context, DownloadsService.class).setAction(DownloadsService.ACTION_ENQUEUE)
                                                                        .putExtra(DownloadsService.EXTRA_REQUEST, request);
context.startService(requestIntent);
```

# Music Playback #
You will find useful our StreamingPlaybackService solving the task of playing music in background. It treats audio focus (e.g. it does not hurt user during incoming calls) and can be controlled by intents or binding to StreamingPlayback interface.