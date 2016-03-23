package cheyikung.com.restaurantfinder;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
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

    private FragmentActivity mActivity;
    private Menu menu;

    private Business business;

    private SupportMapFragment fragment;

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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mActivity = getActivity();
        businessIcon = (WebView) mActivity.findViewById(R.id.detail_business_icon);
        businessName = (TextView) mActivity.findViewById(R.id.detail_business_name);
        businessRating = (TextView) mActivity.findViewById(R.id.detail_business_rating);
        businessReview = (TextView) mActivity.findViewById(R.id.detail_business_reviews);
        businessPhoneNumber = (TextView) mActivity.findViewById(R.id.detail_business_phone_number);
        businessAddress = (TextView) mActivity.findViewById(R.id.detail_business_address);
        business = (Business) getArguments().getSerializable("business");
        isInDatabase = false;

        String imageUrl = business.imageUrl();
        if (imageUrl != null) {
            businessIcon.loadUrl(imageUrl);
        }
        if (business.name() != null) {
            businessName.setText(business.name().toString());
        } else {
            businessName.setText("");
        }

        if (business.rating() != null) {
            businessRating.setText("Rating: " + Double.toString(business.rating()));
        } else {
            businessRating.setText("");
        }

        if (business.reviewCount() != null) {
            businessReview.setText(business.reviewCount().toString() + " Reviews");
        } else {
            businessReview.setText("");
        }

        if (business.phone() != null) {
            businessPhoneNumber.setText(business.phone().toString());
        } else {
            businessPhoneNumber.setText("");
        }

        if (business.location() != null) {
            Location address = business.location();
            ArrayList<String> addressList = address.displayAddress();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < addressList.size(); i++) {
                if (i == addressList.size() - 1) {
                    sb.append("\n");
                    sb.append(addressList.get(i));
                    break;
                }
                sb.append(addressList.get(i));
                sb.append(" ");
            }
            businessAddress.setText(sb.toString());
        } else {
            businessAddress.setText("");
        }

        FragmentManager fm = getChildFragmentManager();
        GoogleMapOptions options = new GoogleMapOptions();
        LatLng latLng = new LatLng(business.location().coordinate().latitude(), business.location().coordinate().longitude());

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
            fragment = SupportMapFragment.newInstance(options);
            fragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    googleMap.addMarker(new MarkerOptions().position(new LatLng(business.location().coordinate().latitude(), business.location().coordinate().longitude()))
                            .title("Marker")
                            .snippet("Please move the marker if needed.")
                            .draggable(true));
                    LatLng latLng;
                    if (business.location() != null) {
                        latLng = new LatLng(business.location().coordinate().latitude(), business.location().coordinate().longitude());
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
            fm.beginTransaction().replace(R.id.detail_map_fragment, fragment).addToBackStack("detail_map").commit();
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
        String businessName;
        String jsonString = new String();
        for (Iterator<QueryRow> it = result; it.hasNext(); ) {
            QueryRow row = it.next();
            Document doc = row.getDocument();
            JSONObject json = new JSONObject(doc.getProperties());
            try {
                if(json.getString("id").toString().equals(business.id().toString())){
                    isInDatabase = true;

                    break;
                }
            } catch (JSONException e) {
                Log.e(DATABASE_NAME, "Cannot write document to database", e);

            }
        }

        Gson gson = new Gson();
        String json = gson.toJson(business);

        JsonElement jelement = new JsonParser().parse(json);
        JsonObject jobject = jelement.getAsJsonObject();

        String bname  = jobject.get("name").getAsString();
        Toast.makeText(mActivity, "business name: " + bname, Toast.LENGTH_SHORT).show();
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
                        jsonmap = new ObjectMapper().readValue(new Gson().toJson(business), HashMap.class);
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
                Toast.makeText(mActivity, "favorite icon", Toast.LENGTH_SHORT).show();
                return true;
            case android.R.id.home:
                getChildFragmentManager().popBackStackImmediate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
