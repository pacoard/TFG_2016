package b105.pgnagent.connectivity;

import android.app.PendingIntent;

import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.content.Context;
import android.app.AlertDialog;
import android.widget.Toast;
import android.os.Handler;

import b105.pgnagent.R;

/**
 * Created by Paco on 02/03/2016.
 *
 * Manages USB connection and receives/transmits data over it.
 */
public class USBHelper implements ConnectionHelper{

    private boolean debug = false; //activates toasts and logs showing debug info

    private boolean doGetNodeList = true; //sends get_node_list message if app is first opened

    private static final String USB_PERMISSION_ACTION = "com.google.android.HID.action.USB_PERMISSION";
    private static final int TRANSFER_TIMEOUT = 1000;
    private static final int COLLECT_DATA_TIME = 100;
    private static final int MAX_PACKET_SIZE = 64; //standard packet size

    // private static final String DEVICE_NAME = "/dev/bus/usb/001/003";
    private static final int VENDOR_ID = 8263;

    private PendingIntent pendingIntentUSBpermission;
    private UsbDevice usbDevicePGN;
    private UsbManager usbManager;
    private UsbEndpoint usbPGN;
    private UsbEndpoint usbSmartphone;
    private UsbDeviceConnection usbConnection;

    private int maxPacketSize;
    public Handler handler = new Handler();

    private Context context; //app context passed
    private PGNMessageProcessor msgProcessor; //necessary to give received bytes

    /**
     * Constructor. Receives app's context and message processor
     *
     * @param msgProcessor PGNMessageProcessor
     * @param context Context
     */
    public USBHelper(Context context, PGNMessageProcessor msgProcessor) {
        this.context = context;
        this.msgProcessor = msgProcessor;
    }

    public void setDoGetNodeList(boolean doGetNodeList) {
        this.doGetNodeList = doGetNodeList;
    }


    /**
     *  Starting method of USBHelper. Sets many needed attributes.
     */
    public void start() {

        //Register possible USB events
        IntentFilter filter = new IntentFilter(USB_PERMISSION_ACTION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        //Register handling actions for each event
        context.registerReceiver(usbEventsManager, filter);

        //Set receiver thread to periodically run
        handler.postDelayed(receiverThreadRunnable, COLLECT_DATA_TIME);
        //Get USB permission intent for broadcast
        pendingIntentUSBpermission = PendingIntent.getBroadcast(this.context, 0, new Intent(USB_PERMISSION_ACTION), 0);

        connect();
    }

    public void stop() {
        //Needs to be in try-catch block since it may execute more than once
        try {
            context.unregisterReceiver(usbEventsManager);
        } catch (IllegalArgumentException e) {
            System.out.println("Receiver already unregistered");
        }

    }

    /**
     * Sets "usbDevicePGN" and "usbDevicePGN"
     */
    public void connect() {

        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        if (usbManager.getDeviceList().size() == 0) {
            //Warn about no USB connected
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setTitle(context.getString(R.string.usb_not_connected_alert));
            alertDialog.setMessage(context.getString(R.string.usb_not_connected_description));
            alertDialog.setCancelable(true);
            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {

                    msgProcessor.finish();
                }
            });
            alertDialog.show();

        } else {
            for (UsbDevice usbDevice : usbManager.getDeviceList().values()) {
                if (/*usbDevice.getDeviceName() == DEVICE_NAME
                        && */usbDevice.getVendorId() == VENDOR_ID) {
                    //Get device
                    usbDevicePGN = usbDevice;
                    //Get USB permission for that device
                    usbManager.requestPermission(usbDevicePGN, pendingIntentUSBpermission);
                }
            }
        }

    }

    /**
     *  Handles USB events
     *  Methods:
     *      onReceive(): distinguishes events and dispatches them by calling setDevice()
     *      setDevice
     */
    private final BroadcastReceiver usbEventsManager = new BroadcastReceiver() {
        /**
         *  Handles USB events
         */
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //permission granted
            if (USB_PERMISSION_ACTION.equals(action)) {
                synchronized (this) {
                    setDevice(intent);
                }
            }
            //device attached
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                synchronized (this) {
                    setDevice(intent);		//Connect to the selected device
                }
                Toast.makeText(context, "Device connected", Toast.LENGTH_SHORT).show();
            }
            //device detached
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (usbDevicePGN != null) {
                    usbDevicePGN = null;
                }
                Toast.makeText(context, "Device disconnected", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         *  Sets "usbConnection", "usbPGN" and "usbSmartphone"
         */
        private void setDevice(Intent intent) {
            usbDevicePGN = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

            if (usbDevicePGN != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                usbConnection = usbManager.openDevice(usbDevicePGN);

                //Interface 3 => PGN transmits data over it
                UsbInterface usbInterface = usbDevicePGN.getInterface(3);
                if (null == usbConnection) {
                    Toast.makeText(context, "setDevice() => unable to establish connection", Toast.LENGTH_SHORT).show();
                } else {
                    if (debug) {
                        Toast.makeText(context, "setDevice() => connection established", Toast.LENGTH_SHORT).show();
                    }
                    //Device connected - claim ownership over the interface
                    usbConnection.claimInterface(usbInterface, true);
                }
                //Direction of end point 1 - OUT - from host to device
                try {
                    if (UsbConstants.USB_DIR_OUT == usbInterface.getEndpoint(0).getDirection()) {
                        usbPGN = usbInterface.getEndpoint(0);
                    }
                } catch (Exception e) {
                    Toast.makeText(context, "usbPGN => Device has no endPointWrite", Toast.LENGTH_SHORT).show();
                }
                //Direction of end point 0 - IN - from device to host
                try {
                    if (UsbConstants.USB_DIR_IN == usbInterface.getEndpoint(1).getDirection()) {
                        usbSmartphone = usbInterface.getEndpoint(1);
                        maxPacketSize = usbSmartphone.getMaxPacketSize();
                    }
                } catch (Exception e) {
                    Toast.makeText(context, "usbSmartphone => Device has no endPointRead", Toast.LENGTH_SHORT).show();
                }
                //Once everything is correctly set, let msgProcessor send "get_node_list" message
                if (doGetNodeList) {
                    msgProcessor.getNodeList();
                    doGetNodeList = false;
                }
            }
        }

    };

    /**
     * Thread that collects received data over a established USB connection.
     * Runs every COLLECT_DATA_TIME miliseconds
     */
    private Runnable receiverThreadRunnable = new Runnable() {
        @Override
        public void run(){
            try {
                if (usbConnection != null && usbSmartphone != null) {	//Verify USB connection and data at end point
                    final byte[] buffer = new byte[maxPacketSize];		//Create new byte buffer every time
                    final int status = usbConnection.bulkTransfer(usbSmartphone, buffer, maxPacketSize, MAX_PACKET_SIZE);

                    if (status >= 0) {
                        msgProcessor.processReceivedMessage(buffer);
                    }
                    else {
                        //gets here every time no message is received.
                    }
                }
            } catch	(Exception e) {
                System.out.println("Exception: " + e.getLocalizedMessage());
            }

            handler.postDelayed(this, COLLECT_DATA_TIME); //execute again after COLLECT_DATA_TIME ms
        }
    };

    public void sendMessage(byte[] tx_buffer ){
        if (usbDevicePGN != null
                && usbPGN != null
                && usbManager.hasPermission(usbDevicePGN))
        {
            int status = usbConnection.bulkTransfer(usbPGN, tx_buffer , tx_buffer .length, TRANSFER_TIMEOUT);  //Send data
            System.out.println("sendMessage() => sent " + status + " bytes");
        }
    }

}

