package b105.pgnagent.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import b105.pgnagent.connectivity.PGNMessageProcessor;
import b105.pgnagent.R;
import b105.pgnagent.models.MessageListViewAdapter;
import b105.pgnagent.models.Node;
import b105.pgnagent.models.NodesListViewAdapter;
import b105.pgnagent.models.PGNMessage;

/**
 * Manages graphic operations and user interaction with nodes
 */
public class WSNActivity extends AppCompatActivity {

    private PGNMessageProcessor processor; //will handle messages
    private  ArrayList<Node> nodes; //nodes and its data
    private ListView nodes_listView;
    private String conn; //"USB" or "BLE"
    private ImageView connImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Forbid landscape mode
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_wsnmap);
        //get nodes list view
        nodes_listView = (ListView) findViewById(R.id.nodes_listView);
        nodes = new ArrayList<>();
        //Get reference to android phone image in activity
        Button phoneImage = (Button)findViewById(R.id.btnWsnOptions);
        //Set layout and content of popup window
        MessageListViewAdapter wsn_Message_listViewAdapter = new MessageListViewAdapter(this, R.layout.listview_item, PGNMessage.PHONE_MESSAGES);
        //Set associated popup window
        setPopUpWindow(phoneImage, wsn_Message_listViewAdapter);
        //Get reference to usb image in activity
        connImage = (ImageView)findViewById(R.id.usbImage);
        //Let user choose Bluetooth or USB:
        askHowToConnect(this);
    }

    @Override
    protected void onPause() {
        //Unset message processor
        if (processor != null) {
            processor.stop();
            processor = null;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Reset message processor
        if (processor == null && conn != null) {
            processor = new PGNMessageProcessor(this, conn);
            processor.start();
        }

    }

    @Override
    protected void onStop() {
        if (processor != null) {
            processor.stop();
            processor = null;
        }
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        //No need for unsetting stuff since this activity is being destroyed.
        super.onBackPressed();
    }

    /**
     * Receives new info for nodes, in case it was updated in NodeDetailActivity
     *
     * @param requestCode int
     * @param resultCode int
     * @param data Intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                nodes =(ArrayList<Node>)  data.getSerializableExtra("nodeList");
                updateSensorListView();
            }
        }
        //reset processor
        processor = new PGNMessageProcessor(this, conn); //needs context so as to be able to update sensors info
        processor.unsetAutomaticGetNodeList();
        processor.start();
    }

    public String getConn() {
        return conn;
    }

    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<Node> nodes) {
        this.nodes = nodes;
        updateSensorListView();
    }

    /**
     * Shows an Alert Dialog asking whether USB or BLE is desired to start WSN management
     *
     * @param context Context
     */
    private void askHowToConnect(final Context context) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(getString(R.string.askHowToConnect_title));

        //Define list of options
        List<CharSequence> list = new LinkedList<CharSequence>();
        list.add(getString(R.string.USB));
        list.add(getString(R.string.BLE));
        final CharSequence conns[] = new CharSequence[list.size()];
        list.toArray(conns);

        //Show options and set what happens when any of them is clicked
        alertDialog.setItems(conns,  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                conn = conns[which].toString();
                processor = new PGNMessageProcessor(context, conn); //needs context so as to be able to update sensors info
                processor.start();
                if (conn.equals(getString(R.string.BLE))) { //set bluetooth image
                    connImage.setImageResource(R.drawable.bluetooth);
                }
            }
        });
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    /**
     * Sets the popup window that will appear if img_view is clicked
     *
     * @param v View
     * @param messageListViewAdapter MessageListViewAdapter
     */
    private void setPopUpWindow(View v, final MessageListViewAdapter messageListViewAdapter) {

        //Set action for imageView's onClick
        v.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                PopupWindow popUp = getPopupWindow(messageListViewAdapter);
                popUp.showAsDropDown(v, -50, -500); // show popup like dropdown list
            }
        });
    }

    /**
     * Returns a PopupWindow given a MessageListViewAdapter containing layout and items info.
     *
     * @param messageListViewAdapter MessageListViewAdapter
     * @return  PopupWindow pw
     */
    private PopupWindow getPopupWindow(MessageListViewAdapter messageListViewAdapter) {

        final PopupWindow pw = new PopupWindow(this);

        // the drop down list is a list view
        final ListView listView = new ListView(this);
        listView.setAdapter(messageListViewAdapter); // set our adapter and pass our pop up window contents

        // set on item selected
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener () {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {

                byte messageCode = PGNMessage.MSG_TYPES_BIMAP.inverse().get(listView.getAdapter().getItem(pos));
                processor.sendMessage(new PGNMessage(
                        (byte) 0x03, //length
                        (byte) 0x00, //id
                        PGNMessage.REQ, //directionCode
                        messageCode, //messageCode
                        new byte[0])
                );
                pw.dismiss();
            }
        });
        // some other visual settings for popup window
        pw.setFocusable(true);
        pw.setWidth(450);
        pw.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        // set the list view as pop up window content
        pw.setContentView(listView);

        return pw;
    }

    /**
     * Updates Sensors List View content and renders view
     */
    private void updateSensorListView() {
        NodesListViewAdapter listViewAdapter = new NodesListViewAdapter(this, R.layout.listview_node, nodes);
        nodes_listView.setAdapter(listViewAdapter);
    }


}
