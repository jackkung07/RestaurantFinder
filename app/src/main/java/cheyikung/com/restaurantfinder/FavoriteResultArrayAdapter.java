package cheyikung.com.restaurantfinder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.yelp.clientlib.entities.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by cheyikung on 3/22/16.
 */
public class FavoriteResultArrayAdapter extends ArrayAdapter<JSONObject> {
    private final Context context;
    private final ArrayList<JSONObject> jsonObjects;

    public FavoriteResultArrayAdapter(Context context, ArrayList<JSONObject> values) {
        super(context, R.layout.list_result, values);
        this.context = context;
        this.jsonObjects = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.list_result, parent, false);
        //Icon
        String imageUrl = null;
        try {
            imageUrl = jsonObjects.get(position).getString("imageUrl");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        WebView webView = (WebView) rowView.findViewById(R.id.list_result_business_icon);
        webView.loadUrl(imageUrl);

        String businessName = null;
        try {
            businessName = jsonObjects.get(position).getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        TextView textViewName = (TextView) rowView.findViewById(R.id.list_result_business_name);
        textViewName.setText(businessName);

        Double rating = null;
        try {
            rating = jsonObjects.get(position).getDouble("rating");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        TextView textViewRating = (TextView) rowView.findViewById(R.id.list_result_business_rating);
        textViewRating.setText("Rating: " + rating.toString());

        JSONObject obj = null;
        try {
            obj = jsonObjects.get(position).getJSONObject("location");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray objArr = null;
        try {
            objArr = obj.getJSONArray("displayAddress");
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        Location address = businesses.get(position).location();
//        ArrayList<String> addressList = address.displayAddress();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < objArr.length(); i++){
            if(i == objArr.length()-1){
                sb.append("\n");
                try {
                    sb.append(objArr.get(i).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            }
            try {
                sb.append(objArr.get(i).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sb.append(" ");
        }
        TextView textViewAddress = (TextView) rowView.findViewById(R.id.list_result_business_address);
        textViewAddress.setText(sb.toString());

        return rowView;
    }
}
