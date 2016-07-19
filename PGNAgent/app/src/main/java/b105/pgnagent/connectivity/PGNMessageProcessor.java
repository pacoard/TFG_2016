package b105.pgnagent.connectivity;

import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;

import b105.pgnagent.R;
import b105.pgnagent.activities.NodeDetailActivity;
import b105.pgnagent.activities.WSNActivity;
import b105.pgnagent.models.Node;
import b105.pgnagent.models.PGNMessage;

/**
 * Deciphers received messages and vice versa.
 *
 * Created by Paco on 10/05/2016.
 */
public class PGNMessageProcessor {

    private boolean debug = true; //activates toasts and logs showing debug info

    private ConnectionHelper connHelper;
    private Context context;

    public static final byte[] GET_NODE_LIST_MESSAGE = { //03 00 10 01 0D0A
            PGNMessage.REQ_GET_LENGTH,
            (byte) 0x00, //id
            PGNMessage.REQ, //directionCode
            PGNMessage.GET_NODE_LIST, //messageCode
            0x0D,
            0x0A
    };

    /**
     * Constructor
     *
     * @param context Context
     * @param connection String
     */
    public PGNMessageProcessor(Context context, String connection) {
        this.context = context;
        if (connection.equals(context.getString(R.string.USB))) {
            connHelper = new USBHelper(context, this);
        } else if (connection.equals(context.getString(R.string.BLE))) {
            connHelper = new BLEHelper(context, this);
        }

    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void getNodeList() {
        connHelper.sendMessage(GET_NODE_LIST_MESSAGE);
    }

    public void start() {
        connHelper.start();
    }

    public void stop() {
        connHelper.stop();
    }

    public void finish() {
        if (context instanceof WSNActivity) {
            ((WSNActivity) context).finish();
        } else if (context instanceof NodeDetailActivity) {
            ((NodeDetailActivity) context).finish();
        }
    }

    /**
     * Most important method, that will determine what to do with
     * any byte buffer received from ConnectionHelper
     *
     * @param rxBuffer byte[]
     */
    public void processReceivedMessage(byte[] rxBuffer) {
        PGNMessage rx_msg = PGNMessage.getPGNMessage(rxBuffer);
        ArrayList<Node> nodeList;
        if (context instanceof WSNActivity) {
            nodeList = ((WSNActivity) context).getNodes();
        } else {
            nodeList = ((NodeDetailActivity) context).getNodes();
        }
        //Actual message content
        byte[] dataPayload = rx_msg.getDataPayload();

        if (rx_msg.getDirectionCode() == PGNMessage.RES) {

            switch (rx_msg.getMessageCode()) {
                //These contain dataPayload
                case PGNMessage.GET_NODE_LIST:
                    handleGetNodeList(rx_msg);
                    break;
                case PGNMessage.GET_NUM_NODES:
                    handleNumNodesNodeList(rx_msg);
                    break;
                case PGNMessage.GET_TEMPERATURE: {
                    NodeDetailActivity nodeActivity = (NodeDetailActivity) context;
                    Node n = nodeActivity.getSelectedNode();
                    n.setTemp(dataPayload[0] * 10 + dataPayload[1]);
                    nodeActivity.setSelectedNode(n);
                    break;
                }
                case PGNMessage.GET_HUMIDITY: {
                    NodeDetailActivity nodeActivity = (NodeDetailActivity) context;
                    Node n = nodeActivity.getSelectedNode();
                    n.setHum(dataPayload[0] * 10 + dataPayload[1]);
                    nodeActivity.setSelectedNode(n);
                    break;
                }
                //These just confirm commands
                case PGNMessage.CLEAR_LED:
                    if(debug) {
                        Toast.makeText(context, "ACK: "+ PGNMessage.MSG_TYPES_BIMAP.get(rx_msg.getMessageCode()), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case PGNMessage.SET_LED:
                    if(debug) {
                        Toast.makeText(context, "ACK: "+ PGNMessage.MSG_TYPES_BIMAP.get(rx_msg.getMessageCode()), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case PGNMessage.TOGGLE_LED:
                    if(debug) {
                        Toast.makeText(context, "ACK: "+ PGNMessage.MSG_TYPES_BIMAP.get(rx_msg.getMessageCode()), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case PGNMessage.SET_ALL_LEDS:
                    if(debug) {
                        Toast.makeText(context, "ACK: "+ PGNMessage.MSG_TYPES_BIMAP.get(rx_msg.getMessageCode()), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case PGNMessage.CLEAR_ALL_LEDS:
                    if(debug) {
                        Toast.makeText(context, "ACK: "+ PGNMessage.MSG_TYPES_BIMAP.get(rx_msg.getMessageCode()), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case PGNMessage.TOGGLE_ALL_LEDS:
                    if(debug) {
                        Toast.makeText(context, "ACK: "+ PGNMessage.MSG_TYPES_BIMAP.get(rx_msg.getMessageCode()), Toast.LENGTH_SHORT).show();
                    }
                    break;

                default: break;
            }
        }
        else { //if (rx_msg.getDirectionCode() == PGNMessage.NUF) should always be true

            switch (rx_msg.getMessageCode()) {

                case PGNMessage.NUF_NEW_NODE:
                    nodeList.add(new Node(rx_msg.getDataPayload()[1], PGNMessage.NOT_SET_VALUE, PGNMessage.NOT_SET_VALUE));
                    break;
                case PGNMessage.NUF_UPDATE_TEMP:
                    nodeList.get(getNodeIndexById(dataPayload[0], nodeList))
                            .setTemp(dataPayload[1]*10 + dataPayload[2]);
                    if (context.getClass().getName().equals("b105.pgnagent.activities.NodeDetailActivity")) {
                        NodeDetailActivity nodeActivity = (NodeDetailActivity) context;
                        Node n = nodeActivity.getSelectedNode();
                        if (n.getId() == dataPayload[0]) {
                            n.setTemp(dataPayload[1] * 10 + dataPayload[2]);
                            nodeActivity.setSelectedNode(n);
                        }
                    }
                    break;
                case PGNMessage.NUF_UPDATE_HUM:
                    nodeList.get(getNodeIndexById(dataPayload[0], nodeList))
                            .setHum(dataPayload[1]*10 + dataPayload[2]);
                    if (context.getClass().getName().equals("b105.pgnagent.activities.NodeDetailActivity")) {
                        NodeDetailActivity nodeActivity = (NodeDetailActivity) context;
                        Node n = nodeActivity.getSelectedNode();
                        if (n.getId() == dataPayload[0]) {
                            n.setHum(dataPayload[1] * 10 + dataPayload[2]);
                            nodeActivity.setSelectedNode(n);
                        }
                    }
                    break;

                default: break;
            }
            updateNodesData(nodeList);
        }


    }

    public void sendMessage(PGNMessage txPGNMessage) {
        connHelper.sendMessage(txPGNMessage.buffer());
    }
    public void sendMessage(byte[] tx_buffer) {
        connHelper.sendMessage(tx_buffer);
    }

    public void updateNodesData(ArrayList<Node> nodeList) {
        if (context instanceof WSNActivity) {
            ((WSNActivity) context).setNodes(nodeList);
        } else if (context instanceof NodeDetailActivity) {
            ((NodeDetailActivity) context).setNodes(nodeList);
        }
    }

    public void unsetAutomaticGetNodeList() {
        connHelper.setDoGetNodeList(false);
    }

    //Manager methods

    private void handleGetNodeList(PGNMessage rx_msg) {
        unsetAutomaticGetNodeList();
        ArrayList<Node> nodes = new ArrayList<>();
        int num_nodes = rx_msg.getDataPayload()[0];

        for (int i = 0; i < num_nodes; i++) {
            nodes.add(new Node(rx_msg.getDataPayload()[i + 1], PGNMessage.NOT_SET_VALUE, PGNMessage.NOT_SET_VALUE));
        }
        updateNodesData(nodes);
    }

    private void handleNumNodesNodeList(PGNMessage rx_msg) {
        byte numNodes = rx_msg.getDataPayload()[0];
        Toast.makeText(context, "Received: There are " + (int) numNodes + " nodes.", Toast.LENGTH_SHORT).show();
    }

    private int getNodeIndexById(byte id, ArrayList<Node> nodeList) {
        for (int i = 0; i < nodeList.size(); i++) {
            if (nodeList.get(i).getId() == (int) id) {
                return i;
            }
        }
        return 0;
    }

}
