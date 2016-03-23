package cheyikung.com.restaurantfinder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.yelp.clientlib.entities.Business;
import com.yelp.clientlib.entities.Location;

import java.util.ArrayList;

/**
 * Created by cheyikung on 3/20/16.
 */
public class SearchResultArrayAdapter extends ArrayAdapter<Business>{
    private final Context context;
    private final ArrayList<Business> businesses;

    public SearchResultArrayAdapter(Context context, ArrayList<Business> values){
        super(context, R.layout.list_result, values);
        this.context = context;
        this.businesses = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.list_result, parent, false);
        //Icon
        String imageUrl = businesses.get(position).imageUrl();
        WebView webView = (WebView) rowView.findViewById(R.id.list_result_business_icon);
        webView.loadUrl(imageUrl);

        String businessName = businesses.get(position).name();
        TextView textViewName = (TextView) rowView.findViewById(R.id.list_result_business_name);
        textViewName.setText(businessName);

        Double rating = businesses.get(position).rating();
        TextView textViewRating = (TextView) rowView.findViewById(R.id.list_result_business_rating);
        textViewRating.setText("Rating: " + rating.toString());

        Location address = businesses.get(position).location();
        ArrayList<String> addressList = address.displayAddress();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < addressList.size(); i++){
            if(i == addressList.size()-1){
                sb.append("\n");
                sb.append(addressList.get(i));
                break;
            }
            sb.append(addressList.get(i));
            sb.append(" ");
        }
        TextView textViewAddress = (TextView) rowView.findViewById(R.id.list_result_business_address);
        textViewAddress.setText(sb.toString());

        return rowView;
    }
}
