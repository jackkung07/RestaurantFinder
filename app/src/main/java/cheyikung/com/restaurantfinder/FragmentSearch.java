package cheyikung.com.restaurantfinder;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
import com.yelp.clientlib.connection.YelpAPI;
import com.yelp.clientlib.connection.YelpAPIFactory;
import com.yelp.clientlib.entities.Business;
import com.yelp.clientlib.entities.SearchResponse;
import com.yelp.clientlib.entities.options.CoordinateOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit.Call;


/**
 * Created by cheyikung on 3/18/16.
 */
public class FragmentSearch extends Fragment {

    //test
    private String nameByGoogle;
    private String addressByGoogle;
    private String attributionsByGoogle;

    private LatLng pickedLocation;
    private GPSTracker gpsTracker;

    private FragmentActivity mActivity;

    private static final int PLACE_PICKER_REQUEST = 1;
//    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
//            new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));

    private SearchView searchRestaurant;
    private ImageButton sortButton;

    private Button pickerButton;
    private ImageButton searchButton;

    private ListView listView;



    private ArrayList<Business> businesses;
    private final int searchLimit = 20;
    private final int radiusInMeter = 16093;
    //search mode 0 : relevance
    //search mode 1 : distance
    private int searchMode = 0;

    private SearchResponse searchResponse;

    public static FragmentSearch newInstance(Context context){
        FragmentSearch fs = new FragmentSearch();
        return fs;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        mActivity = getActivity();


        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // searchRestaurant textedit field
        searchRestaurant = (SearchView) mActivity.findViewById(R.id.searchview_restaurant);
        searchRestaurant.onActionViewExpanded();

        // sort button
        sortButton = (ImageButton) mActivity.findViewById(R.id.imagebutton_sort_mode_button);
        sortButton.setOnClickListener(new View.OnClickListener() {
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
                InputMethodManager imm = (InputMethodManager)mActivity.getSystemService(mActivity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(listView.getWindowToken(), 0);
            }
        });

        //setup list view for restaurant lists
        listView = (ListView) mActivity.findViewById(R.id.result_list);
        listView.setItemsCanFocus(false);
        listView.setClickable(true);

        //get current location

        pickerButton = (Button) mActivity.findViewById(R.id.button_picker_button);

        gpsTracker = new GPSTracker(mActivity);
        if(gpsTracker.canGetLocation()) {
            pickedLocation = gpsTracker.getLatLng();
        }else{
            pickedLocation = new LatLng(37.398160, -122.180831);
            pickerButton.setText("Can't get current location.\nPlease click here to select location");
        }
        gpsTracker.stopUsingGPS();

        //dynamically set place picker button text
        final LatLngBounds currentLocationBound = toBounds(new LatLng(pickedLocation.latitude, pickedLocation.longitude), 20.0);;
        pickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        //search button
        searchButton = (ImageButton) mActivity.findViewById(R.id.imagebutton_search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = mActivity.getApplicationContext();
                String searchRestaurantQuery = searchRestaurant.getQuery().toString();
                Toast.makeText(context, "search button clicked.\nname: " + searchRestaurantQuery + "\nsearch mode: " + Integer.toString(searchMode) + "\nLatLng: " + pickedLocation.toString(), Toast.LENGTH_SHORT).show();

                DownloadYelpDataTask task = new DownloadYelpDataTask();
                task.execute(new String[]{searchRestaurantQuery, Integer.toString(searchMode)});


            }
        });

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
            if(addressByGoogle.length() > 0) {
                pickerButton.setText(addressByGoogle);
            }else{
                pickerButton.setText(nameByGoogle);
            }
            pickerButton.setTextColor(Color.WHITE);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_search, null);
        return root;
    }


    private class DownloadYelpDataTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            YelpAPIFactory apiFactory = new YelpAPIFactory(YelpAPIKey.CONSUMERKEY, YelpAPIKey.CONSUMERSECRET, YelpAPIKey.TOKEN, YelpAPIKey.TOKENSECRET);
            YelpAPI yelpAPI = apiFactory.createAPI();

            Map<String, String> mapParams = new HashMap<>();

            // general params

            mapParams.put("term", params[0]);
            mapParams.put("limit", Integer.toString(searchLimit));
            mapParams.put("sort", params[1]);
            mapParams.put("radius_filter", Integer.toString(radiusInMeter));

            // parameters
            CoordinateOptions coordinate = CoordinateOptions.builder().latitude(pickedLocation.latitude).longitude(pickedLocation.longitude).build();
            Call<SearchResponse> call = yelpAPI.search(coordinate, mapParams);

            try{
                searchResponse = call.execute().body();
                businesses = searchResponse.businesses();
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            listView.setAdapter(new SearchResultArrayAdapter(mActivity, businesses));
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    Business selectedBusiness = (Business) listView.getItemAtPosition(position);
                    Toast toast = Toast.makeText(mActivity, selectedBusiness.name().toString(), Toast.LENGTH_SHORT);
                    toast.show();

                    Fragment fragment = null;
                    Class fragmentClass = FragmentSearchDetail.class;
                    try {
                        fragment = (Fragment) fragmentClass.newInstance();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                    InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(mActivity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(listView.getWindowToken(), 0);

                    Bundle args = new Bundle();
                    args.putSerializable("business", selectedBusiness);

                    //DrawerToggle.setDrawerIndicatorEnabled(false);

                    fragment.setArguments(args);
                    fragmentTransaction.replace(R.id.fragment_container, fragment).addToBackStack(null);
                    fragmentTransaction.commit();
                }
            });
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                }

                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    if (scrollState != 0) {
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(mActivity.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                    }
                }
            });
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(mActivity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(listView.getWindowToken(), 0);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public LatLngBounds toBounds(LatLng center, double radius) {
        LatLng southwest = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 225);
        LatLng northeast = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 45);
        return new LatLngBounds(southwest, northeast);
    }
}
