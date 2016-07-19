package b105.pgnagent.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import b105.pgnagent.connectivity.PGNMessageProcessor;
import b105.pgnagent.R;
import b105.pgnagent.models.Node;
import b105.pgnagent.models.PGNMessage;

/**
 * Shows advanced management and control options associated to
 * a selected node from WSNActivity.
 * When closed, nodes data is updated in WSNActivity.
 */
public class NodeDetailActivity extends AppCompatActivity {

    private PGNMessageProcessor processor;
    private ArrayList<Node> nodes;

    private Node selectedNode;
    private TextView idTextView;
    private TextView tempTextView;
    private TextView humTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Forbid landscape mode
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_node_detail);

        //Get list of nodes from WSNActivity
        nodes = (ArrayList<Node>) getIntent().getSerializableExtra("nodeList");
        selectedNode = nodes.get(getIntent().getIntExtra("selectedNodeIndex", 0));

        //Get UI elements to be set
        idTextView = (TextView) findViewById(R.id.idTextView);
        tempTextView = (TextView) findViewById(R.id.tempTextView);
        humTextView = (TextView) findViewById(R.id.humTextView);

        setNodeData();

        processor = new PGNMessageProcessor(this, getIntent().getStringExtra("conn"));
        processor.unsetAutomaticGetNodeList();
        processor.start();
    }

    public ArrayList<Node> getNodes() {
        return nodes;
    }

    public void setNodes(ArrayList<Node> nodes) {
        this.nodes = nodes;
        setNodeData();
    }

    public Node getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(Node selectedNode) {
        this.selectedNode = selectedNode;
        setNodeData();
    }

    /**
     * Sets all textViews from activity, according to the selected node
     */
    private void setNodeData() {
        idTextView.setText(String.valueOf(selectedNode.getId()));

        if (selectedNode.getTemp() != PGNMessage.NOT_SET_VALUE) {
            tempTextView.setText(String.valueOf(selectedNode.getTemp()) + " ºC");
        } else {
            tempTextView.setText(getResources().getText(R.string.not_known_temperature));
        }

        if (selectedNode.getHum() != PGNMessage.NOT_SET_VALUE) {
            humTextView.setText(String.valueOf(selectedNode.getHum()) + " %");
        } else {
            humTextView.setText(getResources().getText(R.string.not_known_humidity));
        }
    }

    @Override
    public void onBackPressed() {
        //Unregister USB receiver
        //No need for unsetting stuff since this activity is being destroyed.
        Intent intent = new Intent();
        //Modify used node
        nodes.set(getIntent().getIntExtra("selectedNodeIndex", 0) ,selectedNode);
        //Send new list of nodes
        intent.putExtra("nodeList", nodes);

        setResult(RESULT_OK, intent);
        //unset processor
        processor.stop();
        processor = null;
        super.onBackPressed();
    }

    /**
     * Manages click on any button from layout.
     *
     * @param v View
     */
    public void onNodeButtonClick(View v) {
        //Message to be sent
        byte[] payload_bytes = {};
        PGNMessage tx_msg = new PGNMessage(
                PGNMessage.REQ_GET_LENGTH, //length to be set in switch. Will have this value most of the times
                (byte) selectedNode.getId(),
                PGNMessage.REQ,
                (byte) 0x00, //msgCode to be set in switch
                payload_bytes //dataPayload to be set in switch
        );

        switch (v.getId()) {
            case R.id.btnGetTemp:
                tx_msg.setMessageCode(PGNMessage.GET_TEMPERATURE);
                break;
            case R.id.btnGetHum:
                tx_msg.setMessageCode(PGNMessage.GET_HUMIDITY);
                break;
            case R.id.btnSetAllLeds:
                tx_msg.setMessageCode(PGNMessage.SET_ALL_LEDS);
                break;
            case R.id.btnClearAllLeds:
                tx_msg.setMessageCode(PGNMessage.CLEAR_ALL_LEDS);
                break;
            case R.id.btnToggleAllLeds:
                tx_msg.setMessageCode(PGNMessage.TOGGLE_ALL_LEDS);
                break;
            case R.id.btnSetLed1:
                tx_msg.setPgnLength((byte) (tx_msg.getPgnLength()+ 1));
                tx_msg.setMessageCode(PGNMessage.SET_LED);
                payload_bytes = new byte[] {(byte) 0x01};
                tx_msg.setDataPayload(payload_bytes);
                break;
            case R.id.btnSetLed2:
                tx_msg.setPgnLength((byte) (tx_msg.getPgnLength()+ 1));
                tx_msg.setMessageCode(PGNMessage.SET_LED);
                payload_bytes = new byte[] {(byte) 0x02};
                tx_msg.setDataPayload(payload_bytes);
                break;
            case R.id.btnClearLed1:
                tx_msg.setPgnLength((byte) (tx_msg.getPgnLength()+ 1));
                tx_msg.setMessageCode(PGNMessage.CLEAR_LED);
                payload_bytes = new byte[] {(byte) 0x01};
                tx_msg.setDataPayload(payload_bytes);
                break;
            case R.id.btnClearLed2:
                tx_msg.setPgnLength((byte) (tx_msg.getPgnLength()+ 1));
                tx_msg.setMessageCode(PGNMessage.CLEAR_LED);
                payload_bytes = new byte[] {(byte) 0x02};
                tx_msg.setDataPayload(payload_bytes);
                break;
            case R.id.btnToggleLed1:
                tx_msg.setPgnLength((byte) (tx_msg.getPgnLength()+ 1));
                tx_msg.setMessageCode(PGNMessage.TOGGLE_LED);
                payload_bytes = new byte[] {(byte) 0x01};
                tx_msg.setDataPayload(payload_bytes);
                break;
            case R.id.btnToggleLed2:
                tx_msg.setPgnLength((byte) (tx_msg.getPgnLength()+ 1));
                tx_msg.setMessageCode(PGNMessage.TOGGLE_LED);
                payload_bytes = new byte[] {(byte) 0x02};
                tx_msg.setDataPayload(payload_bytes);
                break;

            default: break;
        }

        processor.sendMessage(tx_msg);
    }

}
