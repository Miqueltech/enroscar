`SDKDependentUtils` class can create a request builder instance for you depending on the actual Android framework version running your application.
It uses a custom implementation for Eclair and Gingerbread that is based on `Notification.Builder` source.
It wraps calls to `Notification.Builder` for Honeycomb.

Usage (taken from `DownloadsService`):
```
  final Notification n = AppUtils.getSdkDependentUtils().createNotificationBuilder(DownloadsService.this)
      .setWhen(notificationTime)
      .setSmallIcon(android.R.drawable.stat_sys_download)
      .setTicker(request.title)
      .setContentTitle(request.title)
      .setContentText(request.description)
      .setContentIntent(PendingIntent.getBroadcast(DownloadsService.this, 0, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT))
      .setOngoing(true)
      .setProgress(max, (int)(p * max), progress == null)
      .getNotification();
```