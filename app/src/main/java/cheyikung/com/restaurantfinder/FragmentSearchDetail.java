package cheyikung.com.restaurantfinder;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yelp.clientlib.entities.Business;
import com.yelp.clientlib.entities.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by cheyikung on 3/20/16.
 */
public class FragmentSearchDetail extends Fragment {

    private WebView businessIcon;
    private TextView businessName;
    private TextView businessRating;
    private TextView businessReview;
    private TextView businessPhoneNumber;
    private TextView businessAddress;

    private Activity mActivity;
    private Menu menu;

//    private Business business;
    private JSONObject business;
    private String businessJsonStr;

    private MapFragment fragment;

    private Manager manager;
    private Database database;

    private static View view;

    private final String DATABASE_NAME = "favorite_db";

    private boolean isInDatabase;

    public static FragmentSearchDetail newInstance(Context context) {
        FragmentSearchDetail fsd = new FragmentSearchDetail();
        return fsd;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isInDatabase", isInDatabase);
        outState.putString("businessJsonStr", businessJsonStr);
//        outState.putSerializable("businesses", business);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mActivity = getActivity();
        businessIcon = (WebView) mActivity.findViewById(R.id.detail_business_icon);
        businessName = (TextView) mActivity.findViewById(R.id.detail_business_name);
        businessRating = (TextView) mActivity.findViewById(R.id.detail_business_rating);
        businessReview = (TextView) mActivity.findViewById(R.id.detail_business_reviews);
        businessPhoneNumber = (TextView) mActivity.findViewById(R.id.detail_business_phone_number);
        businessAddress = (TextView) mActivity.findViewById(R.id.detail_business_address);
//        business = (Business) getArguments().getSerializable("business");
        businessJsonStr = getArguments().getString("businessJsonStr");

        try {
            business = new JSONObject(businessJsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        isInDatabase = false;
        if(savedInstanceState!=null){
            isInDatabase = savedInstanceState.getBoolean("isInDatabase");
        }

        String imageUrl = null;
        try {
            imageUrl = business.getString("imageUrl");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (imageUrl != null) {
            businessIcon.loadUrl(imageUrl);
            businessIcon.zoomBy(0.05f);
        }

        String name = null;
        try {
            name = business.getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (name != null) {
            businessName.setText(name);
        } else {
            businessName.setText("");
        }

        Double rating = null;
        try {
            rating = business.getDouble("rating");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (rating != null) {
            businessRating.setText("Rating: " + Double.toString(rating));
        } else {
            businessRating.setText("");
        }

        int reviewCount = 0;
        try {
            reviewCount = business.getInt("reviewCount");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (reviewCount != 0) {
            businessReview.setText(Integer.toString(reviewCount) + " Reviews");
        } else {
            businessReview.setText("");
        }

        String phone = null;
        try {
            phone = business.getString("phone");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (phone != null) {
            businessPhoneNumber.setText(phone);
        } else {
            businessPhoneNumber.setText("");
        }
        JSONObject location = null;
        try {
            location = business.getJSONObject("location");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray displayAddress = null;
        if(location != null){
            try {
                displayAddress = location.getJSONArray("displayAddress");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < displayAddress.length(); i++) {
                if (i == displayAddress.length() - 1) {
                    sb.append("\n");
                    try {
                        sb.append(displayAddress.getString(i));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                try {
                    sb.append(displayAddress.getString(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                sb.append(" ");
            }
            businessAddress.setText(sb.toString());
        }else {
            businessAddress.setText("");
        }


//        if (business.location() != null) {
//            Location address = business.location();
//            ArrayList<String> addressList = address.displayAddress();
//            StringBuilder sb = new StringBuilder();
//            for (int i = 0; i < addressList.size(); i++) {
//                if (i == addressList.size() - 1) {
//                    sb.append("\n");
//                    sb.append(addressList.get(i));
//                    break;
//                }
//                sb.append(addressList.get(i));
//                sb.append(" ");
//            }
//            businessAddress.setText(sb.toString());
//        } else {
//            businessAddress.setText("");
//        }


        GoogleMapOptions options = new GoogleMapOptions();
        JSONObject coordinate = null;
        try {
            coordinate = location.getJSONObject("coordinate");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        LatLng latLng = null;
        if(coordinate != null) {

            try {
                latLng = new LatLng(coordinate.getDouble("latitude"), coordinate.getDouble("longitude"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng) // Center Set
                .zoom(11.0f)                // Zoom
                .bearing(90)                // Orientation of the camera to east
                .tilt(0)                   // Tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        options.mapType(GoogleMap.MAP_TYPE_NORMAL)
                .compassEnabled(false)
                .rotateGesturesEnabled(false)
                .tiltGesturesEnabled(false).camera(cameraPosition).liteMode(true);
        if (fragment == null) {
            fragment = MapFragment.newInstance(options);
            final JSONObject finalCoordinate = coordinate;
            fragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    try {
                        googleMap.addMarker(new MarkerOptions().position(new LatLng(finalCoordinate.getDouble("latitude"), finalCoordinate.getDouble("longitude")))
                                .title("Marker")
                                .snippet("Please move the marker if needed.")
                                .draggable(true));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    LatLng latLng = null;
                    if (finalCoordinate != null) {
                        try {
                            latLng = new LatLng(finalCoordinate.getDouble("latitude"), finalCoordinate.getDouble("longitude"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        latLng = new LatLng(0, 0);
                    }
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(latLng) // Center Set
                            .zoom(15.0f)                // Zoom
                            .bearing(90)                // Orientation of the camera to east
                            .tilt(0)                   // Tilt of the camera to 30 degrees
                            .build();                   // Creates a CameraPosition from the builder
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                }
            });

            getChildFragmentManager().beginTransaction().add(R.id.detail_map_fragment,fragment).commit();
        }

        try {
            manager = new Manager(new AndroidContext(mActivity), Manager.DEFAULT_OPTIONS);
            Log.d(DATABASE_NAME, "Manager created");
        } catch (IOException e) {
            Log.e(DATABASE_NAME, "Cannot create manager object");
            return;
        }

        if (!Manager.isValidDatabaseName(DATABASE_NAME)) {
            Log.e(DATABASE_NAME, "Bad database name");
            return;
        }

        try {
            if(manager.getExistingDatabase(DATABASE_NAME)!=null){
                database = manager.getExistingDatabase(DATABASE_NAME);
                Log.d (DATABASE_NAME, "Database exists");
            }else {
                database = manager.getDatabase(DATABASE_NAME);
                Log.d(DATABASE_NAME, "Database created");
            }

        } catch (CouchbaseLiteException e) {
            Log.e(DATABASE_NAME, "Cannot get database");
            return;
        }

        // check if data exists in database
        Query query = database.createAllDocumentsQuery();
        query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
        QueryEnumerator result = null;
        try {
            result = query.run();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        for (Iterator<QueryRow> it = result; it.hasNext(); ) {
            QueryRow row = it.next();
            Document doc = row.getDocument();
            JSONObject json = new JSONObject(doc.getProperties());
            try {
                if(json.getString("id").toString().equals(business.getString("id"))){
                    isInDatabase = true;
                    break;
                }
            } catch (JSONException e) {
                Log.e(DATABASE_NAME, "Cannot write document to database", e);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(parent);
            }
        }
        try {
            view = inflater.inflate(R.layout.fragment_search_detail, null);
        } catch (InflateException e) {

        }
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.toolbar_menu, menu);
        if(isInDatabase){
            menu.getItem(0).setIcon(mActivity.getDrawable(R.drawable.ic_favorite_white_24dp));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.toolbar_menu_favorite:
                if(isInDatabase==false){
                    try {
                        if(manager.getExistingDatabase(DATABASE_NAME)!=null){
                            database = manager.getExistingDatabase(DATABASE_NAME);
                            Log.d (DATABASE_NAME, "Database exists");
                        }else {
                            database = manager.getDatabase(DATABASE_NAME);
                            Log.d(DATABASE_NAME, "Database created");
                        }

                    } catch (CouchbaseLiteException e) {
                        Log.e(DATABASE_NAME, "Cannot get database");

                    }

                    Map<String,Object> jsonmap = null;
                    try {
//                        jsonmap = new ObjectMapper().readValue(new Gson().toJson(business), HashMap.class);
                        jsonmap = new ObjectMapper().readValue(businessJsonStr, HashMap.class);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Document document = database.createDocument();
                    try {
                        document.putProperties(jsonmap);
                    } catch (CouchbaseLiteException e) {
                        Log.e(DATABASE_NAME, "Cannot write document to database", e);

                    }
                    menu.getItem(0).setIcon(mActivity.getDrawable(R.drawable.ic_favorite_white_24dp));
                }
                Toast.makeText(mActivity, "Place saved", Toast.LENGTH_SHORT).show();
                return true;
            case android.R.id.home:
                getFragmentManager().popBackStackImmediate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
