package b105.pgnagent.models;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import b105.pgnagent.activities.NodeDetailActivity;
import b105.pgnagent.R;
import b105.pgnagent.activities.WSNActivity;

/**
 * Defines style (from a XML layout file) and a list of items for a ListView intended to show sensors info and its options.
 *
 * Created by Paco on 04/05/2016.
 */
public class NodesListViewAdapter extends ArrayAdapter<Node> {

    private final static int LAYOUT_FILE = R.layout.listview_node;
    private final String TAG = NodesListViewAdapter.class.getSimpleName();

    private Activity activity;
    private ArrayList<Node> arrayList;

    /**
     * Constructor
     *
     * @param activity Activity
     * @param resource int
     * @param arrayList ArrayList<Node>
     */
    public NodesListViewAdapter(Activity activity, int resource , ArrayList<Node> arrayList) {
        super(activity, resource, arrayList);
        this.activity = activity;
        this.arrayList = arrayList;
        Log.i(TAG, "NodesListViewAdapter constructor.");
    }

    public Activity getActivity() {
        return activity;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
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

        // set sensor data
        viewHolder.id_text.setText("ID: " + String.valueOf(arrayList.get(position).getId()));
        viewHolder.temp_text.setText(R.string.not_known_temperature);
        viewHolder.hum_text.setText(R.string.not_known_humidity);
        if (arrayList.get(position).getTemp() != PGNMessage.NOT_SET_VALUE) {
            viewHolder.temp_text.setText("Temp: " + String.valueOf(arrayList.get(position).getTemp()) + " ºC");
        }
        if (arrayList.get(position).getHum() != PGNMessage.NOT_SET_VALUE) {
            viewHolder.hum_text.setText("Hum: " + String.valueOf(arrayList.get(position).getHum()) + " %");
        }

        //Launch NodeDetailActivity when node item is clicked
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, NodeDetailActivity.class);
                intent.putExtra("nodeList", arrayList);
                intent.putExtra("selectedNodeIndex", position);
                intent.putExtra("conn", ((WSNActivity) activity).getConn());
                activity.startActivityForResult(intent, 1);
            }
        });
        // the view must be returned to our activity
        return convertView;
    }

    /**
     * Parent view of this ListView instance
     */
    private class ViewHolder {
        private TextView id_text;
        private ImageView node_imageView;
        private TextView temp_text;
        private TextView hum_text;

        public ViewHolder(View v) {
            id_text = (TextView) v.findViewById(R.id.idTextView);
            node_imageView = (ImageView) v.findViewById(R.id.sensorImageView);
            temp_text = (TextView) v.findViewById(R.id.tempTextView);
            hum_text = (TextView) v.findViewById(R.id.humTextView);
        }
    }
}
