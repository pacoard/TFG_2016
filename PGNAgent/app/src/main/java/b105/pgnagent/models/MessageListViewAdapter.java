package b105.pgnagent.models;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

import b105.pgnagent.R;

//With the help of: http://www.devexchanges.info/2015/02/android-popupwindow-show-as-dropdown.html

/**
 * Defines style (from a XML layout file) and a list of items for a ListView intended to show messages.
 *
 * Created by Paco on 04/05/2016.
 */
public class MessageListViewAdapter extends ArrayAdapter<String> {

    private final static int LAYOUT_FILE = R.layout.listview_item;
    private final String TAG = MessageListViewAdapter.class.getSimpleName();

    private Activity activity;
    private ArrayList<String> arrayList;

    /**
     * Constructor
     *
     * @param activity Activity
     * @param resource int
     * @param arrayList ArrayList<String>
     */
    public MessageListViewAdapter(Activity activity, int resource , ArrayList<String> arrayList) {
        super(activity, resource, arrayList);
        this.activity = activity;
        this.arrayList = arrayList;
        Log.i(TAG, "MessageListViewAdapter constructor.");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        // inflate layout from xml
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        // If holder does not exist, locate the whole view from UI file.
        if (convertView == null) {
            // inflate UI from XML file
            convertView = inflater.inflate(LAYOUT_FILE, parent, false);
            // get all UI view
            viewHolder = new ViewHolder(convertView);
            // set tag for holder
            convertView.setTag(viewHolder);
        } else {
            // if holder created, get tag from view
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // set wgn_message data
        viewHolder.message_text.setText(arrayList.get(position));
        // the view must be returned to our activity
        return convertView;
    }

    /**
     * Parent view of this ListView instance
     */
    private class ViewHolder {
        private TextView message_text;

        public ViewHolder(View v) {
            message_text = (TextView) v.findViewById(R.id.message_text);
        }
    }
}
