# Declare application service #

Put the following lines to your `AndroidManifest.xml` file.

```
	...
	<application>
	    
	    ...

	    <!-- Enroscar application service declaration. -->    
        <service
            android:name="com.stanfy.app.service.ApplicationService"
            android:exported="false" >
            <intent-filter>
                <action android:name="com.stanfy.app.service.ApiMethods" />
            </intent-filter>
        </service>

        ...

    </application>

```

# Use `SimpleRequestBuilder` #

Use `SimpleRequestBuilder` for creating Android loader that uses the declared service for performing requests to the remote server API.
Please, go through [Android loaders docs](http://developer.android.com/guide/components/loaders.html) to refresh in mind how to use loaders.

```
	
public class ExampleFragment extends Fragment implements LoaderCallbacks<ResponseData<Profile>> {

  public static class Profile {
    private String name;
    private String description;

    public String getName() { return name; }
    public String getDescription() { return description; }
  }

  private static final int LOADER_ID = 1;

  @Override
  public void onActivityCreated(final Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    getLoaderManager().initLoader(LOADER_ID, null, this);
  }

  @Override
  public Loader<ResponseData<Profile>> onCreateLoader(final int id, final Bundle args) {
    return new SimpleRequestBuilder<Profile>(getActivity()) { }
      .setUrl("https://api.twitter.com/1/users/show.json")
      .setFormat("json")
      .addParam("screen_name", "TwitterAPI")
      .getLoader();
  }

  @Override
  public void onLoadFinished(final Loader<ResponseData<Profile>> loader, final ResponseData<Profile> response) {
    if (response.isSuccessful()) {
      GUIUtils.shortToast(getActivity(), profile.getName() + " / " + profile.getDescription());
    }
  }

  @Override
  public void onLoaderReset(final Loader<ResponseData<Profile>> loader) {
    // nothing
  }

}

```