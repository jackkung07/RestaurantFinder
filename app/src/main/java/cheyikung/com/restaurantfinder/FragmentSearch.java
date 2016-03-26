package cheyikung.com.restaurantfinder;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;


import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.Gson;
import com.google.maps.android.SphericalUtil;
import com.yelp.clientlib.connection.YelpAPI;
import com.yelp.clientlib.connection.YelpAPIFactory;
import com.yelp.clientlib.entities.Business;
import com.yelp.clientlib.entities.SearchResponse;
import com.yelp.clientlib.entities.options.CoordinateOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit.Call;

import static android.view.View.*;


/**
 * Created by cheyikung on 3/18/16.
 */
public class FragmentSearch extends Fragment implements ConnectionCallbacks, OnConnectionFailedListener {

    //test
    private String nameByGoogle;
    private String addressByGoogle;
    private String attributionsByGoogle;

    private LatLng pickedLocation;
    private LocationManager locationManager;
    private Location loc;
    private LatLngBounds currentLocationBound;

    private Activity mActivity;

    private GoogleApiClient mGoogleApiClient;
    private static final int PLACE_PICKER_REQUEST = 1;

    private SearchView searchRestaurant;
    private ImageButton sortButton;

    private Button pickerButton;
    private ImageButton searchButton;

    private ListView listView;

    private ArrayList<Business> businesses;
    private String searchRestaurantQuery;
    private final int searchLimit = 20;
    private final int radiusInMeter = 16093;
    //search mode 0 : relevance
    //search mode 1 : distance
    private int searchMode;
    private boolean hasSearched;

    private SearchResponse searchResponse;

    private YelpAPIFactory apiFactory;
    private YelpAPI yelpAPI;
    private Map<String, String> mapParams;


    private Fragment fragmentSearchDetail;
    private Class fragmentClass;

    public static FragmentSearch newInstance(Context context) {
        FragmentSearch fs = new FragmentSearch();
        return fs;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setRetainInstance(true);
        mActivity = getActivity();
        if (savedInstanceState == null) {
            searchMode = 0;
            hasSearched = false;
        }

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // searchRestaurant searchView field
        searchRestaurant = (SearchView) mActivity.findViewById(R.id.searchview_restaurant);
        searchRestaurant.onActionViewExpanded();
        searchRestaurant.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Context context = mActivity.getApplicationContext();
                Toast.makeText(context, "Type in anything", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        if (savedInstanceState != null) {
            searchRestaurant.setQuery(searchRestaurantQuery, false);
        }

        // sort button
        sortButton = (ImageButton) mActivity.findViewById(R.id.imagebutton_sort_mode_button);
        if (searchMode == 0) {
            sortButton.setImageResource(R.drawable.ic_local_library_white_24dp);
        } else {
            sortButton.setImageResource(R.drawable.ic_directions_run_white_24dp);
        }
        sortButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = mActivity.getApplicationContext();
                if (searchMode == 0) {
                    searchMode = 1;
                    sortButton.setImageResource(R.drawable.ic_directions_run_white_24dp);
                    Toast.makeText(context, "Search by distance", Toast.LENGTH_SHORT).show();

                } else {
                    searchMode = 0;
                    sortButton.setImageResource(R.drawable.ic_local_library_white_24dp);
                    Toast.makeText(context, "Search by relevance", Toast.LENGTH_SHORT).show();
                }
                if (businesses != null && businesses.size() > 0) {
                    searchButton.performClick();
                }
            }
        });
        sortButton.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Context context = mActivity.getApplicationContext();
                if (searchMode == 0) {
                    Toast.makeText(context, "Sort by relevance\nPress to toggle between sort by relevance or by sort by distance", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Sort by distance\nPress to toggle between sort by relevance or by sort by distance", Toast.LENGTH_LONG).show();
                }
                return false;
            }
        });

        //setup list view for restaurant lists
        listView = (ListView) mActivity.findViewById(R.id.result_list);
        listView.setItemsCanFocus(false);
        listView.setClickable(true);

        //get current location
        pickerButton = (Button) mActivity.findViewById(R.id.button_picker_button);

        pickerButton.setOnLongClickListener(new OnLongClickListener() {
            Context context = mActivity.getApplicationContext();

            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(context, "Click here to select location", Toast.LENGTH_LONG).show();
                return false;
            }
        });

        if (savedInstanceState == null) {
            currentLocation();
            if (pickedLocation == null) {
                pickedLocation = new LatLng(37.398160, -122.180831);
                pickerButton.setText("Can't get current location.\nPlease click here to select location");
            }
        }

        if (savedInstanceState != null) {
            pickerButton.setText(savedInstanceState.getString("pickerButtonText"));
        }

        //dynamically set place picker button text
        currentLocationBound = toBounds(new LatLng(pickedLocation.latitude, pickedLocation.longitude), 20.0);

        pickerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.getFragmentManager().beginTransaction().detach(mActivity.getFragmentManager().findFragmentByTag("fragment_search")).commit();
                Context context = mActivity.getApplicationContext();
                Toast.makeText(context, "Please wait", Toast.LENGTH_LONG).show();
                try {
                    PlacePicker.IntentBuilder intentBuilder =
                            new PlacePicker.IntentBuilder();
                    intentBuilder.setLatLngBounds(currentLocationBound);
                    Intent intent = intentBuilder.build(mActivity);
                    startActivityForResult(intent, PLACE_PICKER_REQUEST);

                } catch (GooglePlayServicesRepairableException
                        | GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                mActivity.getFragmentManager().beginTransaction().attach(mActivity.getFragmentManager().findFragmentByTag("fragment_search")).commit();

            }
        });

        //search button
        searchButton = (ImageButton) mActivity.findViewById(R.id.imagebutton_search_button);
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = mActivity.getApplicationContext();
                searchRestaurantQuery = searchRestaurant.getQuery().toString();
                hasSearched = true;
                DownloadYelpDataTask task = new DownloadYelpDataTask();
                task.execute(new String[]{searchRestaurantQuery, Integer.toString(searchMode), Double.toString(pickedLocation.latitude), Double.toString(pickedLocation.longitude)});

                InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(mActivity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchRestaurant.getWindowToken(), 0);
            }

        });


        searchRestaurant.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.e("onQueryTextChange", "called");
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {

                Log.e("onQueryTextSubmit", "called");
                // Do your task here
                searchButton.performClick();
                return false;
            }

        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            searchMode = savedInstanceState.getInt("searchMode");
            pickedLocation = savedInstanceState.getParcelable("pickedLocation");
            searchRestaurantQuery = savedInstanceState.getString("searchRestaurantQuery");
            hasSearched = savedInstanceState.getBoolean("hasSearched");
            if (hasSearched) {
                searchButton.performClick();
            }
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("pickerButtonText", pickerButton.getText().toString());
        outState.putInt("searchMode", searchMode);
        outState.putParcelable("pickedLocation", pickedLocation);
        outState.putString("searchRestaurantQuery", searchRestaurantQuery);
        outState.putBoolean("hasSearched", hasSearched);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onActivityResult(int requestCode,
                                 int resultCode, Intent data) {

        if (requestCode == PLACE_PICKER_REQUEST
                && resultCode == Activity.RESULT_OK) {

            final Place place = PlacePicker.getPlace(mActivity, data);
            this.pickedLocation = place.getLatLng();
            this.nameByGoogle = place.getName().toString();
            this.addressByGoogle = place.getAddress().toString();
            this.attributionsByGoogle = (String) place.getAttributions();
            if (this.attributionsByGoogle == null) {
                this.attributionsByGoogle = "";
            }
            if (addressByGoogle.length() > 0) {
                pickerButton.setText(addressByGoogle);
            } else {
                pickerButton.setText(nameByGoogle);
            }
            pickerButton.setTextColor(Color.WHITE);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_search, null);
        return view;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("google api error", "Google Places API connection failed with error code: "
                + connectionResult.getErrorCode());

//        Toast.makeText(mActivity,
//                "Google Places API connection failed with error code:" +
//                        connectionResult.getErrorCode(),
//                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("get current place", "Google Api Client connected.");

        if (mGoogleApiClient != null) {
            Log.d("get current place", "Getting nearby places...");

            PendingResult<PlaceLikelihoodBuffer> result =
                    Places.PlaceDetectionApi.getCurrentPlace(mGoogleApiClient, null);

            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                    Log.d("get current place", "Got results: " + likelyPlaces.getCount() + " place found.");

                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        Log.i("get current place", String.format("Place '%s' has likelihood: %g",
                                placeLikelihood.getPlace().getName(),
                                placeLikelihood.getLikelihood()));
                    }

                    likelyPlaces.release();
                }
            });
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private void currentLocation() {
        locationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);

        String provider = locationManager.getBestProvider(new Criteria(), false);

        Location location = locationManager.getLastKnownLocation(provider);

        if (location == null) {
            locationManager.requestLocationUpdates(provider, 0, 0, listener);
        } else {
            pickedLocation = new LatLng(location.getLatitude(), location.getLongitude());
        }

    }

    private LocationListener listener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e("location update", "location update : " + location);
            loc = location;
            locationManager.removeUpdates(listener);
        }
    };


    private class DownloadYelpDataTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            apiFactory = new YelpAPIFactory(YelpAPIKey.CONSUMERKEY, YelpAPIKey.CONSUMERSECRET, YelpAPIKey.TOKEN, YelpAPIKey.TOKENSECRET);
            yelpAPI = apiFactory.createAPI();

            mapParams = new HashMap<>();

            // general params
            mapParams.put("term", params[0]);
            mapParams.put("limit", Integer.toString(searchLimit));
            mapParams.put("sort", params[1]);
            mapParams.put("radius_filter", Integer.toString(radiusInMeter));
            mapParams.put("latitude", params[2]);
            mapParams.put("longitude", params[3]);

            // parameters
            CoordinateOptions coordinate = CoordinateOptions.builder().latitude(pickedLocation.latitude).longitude(pickedLocation.longitude).build();
            Call<SearchResponse> call = yelpAPI.search(coordinate, mapParams);

            try {
                searchResponse = call.execute().body();
                businesses = searchResponse.businesses();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            listView.setAdapter(new SearchResultArrayAdapter(mActivity, businesses));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    Business selectedBusiness = (Business) listView.getItemAtPosition(position);
                    Toast toast = Toast.makeText(mActivity, selectedBusiness.name().toString(), Toast.LENGTH_SHORT);
                    toast.show();


                    fragmentClass = FragmentSearchDetail.class;
                    try {
                        fragmentSearchDetail = (Fragment) fragmentClass.newInstance();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    FragmentManager fragmentManager = mActivity.getFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                    InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(mActivity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchRestaurant.getWindowToken(), 0);


                    ObjectMapper mapper = new ObjectMapper();
                    String businessJson;

                    Gson gson = new Gson();
                    businessJson = gson.toJson(selectedBusiness);
                    JSONObject jsonObjectobj = null;
                    try {
                        jsonObjectobj = new JSONObject(businessJson);
                        jsonObjectobj.put("phone", selectedBusiness.phone());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    String businessJsonStr = jsonObjectobj.toString();

                    Bundle args = new Bundle();

//                    args.putSerializable("business", jsonObjectobj);
                    args.putString("businessJsonStr", businessJsonStr);
                    if (selectedBusiness != null) {
                        Log.d("bussiness not null", "not null");
                    }

                    fragmentSearchDetail.setArguments(args);
                    fragmentTransaction.hide(getFragmentManager().findFragmentByTag("fragment_search")).add(R.id.fragment_container, fragmentSearchDetail, null).addToBackStack(null).commit();
                }
            });
        }
    }

    public LatLngBounds toBounds(LatLng center, double radius) {
        LatLng southwest = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 225);
        LatLng northeast = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 45);
        return new LatLngBounds(southwest, northeast);
    }
}
