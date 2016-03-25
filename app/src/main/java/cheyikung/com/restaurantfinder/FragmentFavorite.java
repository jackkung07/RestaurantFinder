package cheyikung.com.restaurantfinder;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by cheyikung on 3/19/16.
 */
public class FragmentFavorite extends Fragment {
    Activity mActivity;
    AppCompatActivity mAppCompatActivity;
    private ListView listView;

    private Manager manager;
    private Database database;
    private final String DATABASE_NAME = "favorite_db";

    private ArrayList<JSONObject> jsonObjectList;

    private Menu menu;


    public static FragmentFavorite newInstance(Context context){
        FragmentFavorite ff = new FragmentFavorite();
        return ff;
    }



    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setRetainInstance(true);
        super.onViewCreated(view, savedInstanceState);
        mActivity = getActivity();
        listView = (ListView) mActivity.findViewById(R.id.favorite_list);
        listView.setItemsCanFocus(false);
        listView.setClickable(false);

        GetDataFromDatabase task = new GetDataFromDatabase();
        task.execute(new String());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_favorite, null);
        setHasOptionsMenu(false);
        return root;
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.toolbar_menu, menu);
        menu.getItem(0).setIcon(mActivity.getDrawable(R.drawable.ic_favorite_border_white_24dp));
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getFragmentManager().popBackStackImmediate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class GetDataFromDatabase extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... params) {
            try {
                manager = new Manager(new AndroidContext(mActivity), Manager.DEFAULT_OPTIONS);
                Log.d(DATABASE_NAME, "Manager created");
            } catch (IOException e) {
                Log.e(DATABASE_NAME, "Cannot create manager object");
                return null;
            }

            if (!Manager.isValidDatabaseName(DATABASE_NAME)) {
                Log.e(DATABASE_NAME, "Bad database name");
                return null;
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
                return null;
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
            jsonObjectList = new ArrayList<JSONObject>();
            for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                QueryRow row = it.next();
                Document doc = row.getDocument();
                JSONObject json = new JSONObject(doc.getProperties());
                jsonObjectList.add(json);

            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            listView.setAdapter(new FavoriteResultArrayAdapter(mActivity, jsonObjectList));
        }
    }
}
