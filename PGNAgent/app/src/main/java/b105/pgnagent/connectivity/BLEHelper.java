package b105.pgnagent.connectivity;

import android.app.Activity;
import android.bluetooth.*;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import b105.pgnagent.R;

/**
 * Created by Paco on 10/06/2016.
 *
 * Manages Bluetooth Low Energy connection and receives/transmits data over it.
 */
public class BLEHelper implements ConnectionHelper{

    private final String HM10_ADDRESS = "7C:EC:79:DB:3A:C4";
    /**
     * More than enough time to find HM-10 module, and enough of
     * waiting time until and Alert Dialog shows when it is not found.
     * Scanning is set to SCAN_MODE_LOW_LATENCY, so that connection
     * establishes pretty fast.
     */
    private static final long SCAN_PERIOD = 750;
    private static final long WAITING_TIME_GET_NODE_LIST = 100;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic mCharacteristic; //send/receive data with this
    private boolean doGetNodeList = true; //sends get_node_list message if app is first opened

    //BLEHelper object attributes
    private Context context; //app context passed
    private PGNMessageProcessor msgProcessor; //necessary to pass received bytes


    /**
     * Constructor
     *
     * @param context Context
     * @param msgProcessor PGNMessageProcessor
     */
    public BLEHelper(Context context, PGNMessageProcessor msgProcessor) {
        this.context = context;
        this.msgProcessor = msgProcessor;
    }

    public void setDoGetNodeList(boolean doGetNodeList) {
        this.doGetNodeList = doGetNodeList;
    }


    /**
     * Begins BLE scanning and connecting process by setting up a few things
     */
    public void start() {
        //Determine whether BLE is supported on the device
        mHandler = new Handler();
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(context, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            //finish();
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            showErrorDialog(context.getString(R.string.ble_not_connected_alert),
                    context.getString(R.string.ble_not_connected_description));
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) //set connection as fast as possible
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);
        }
    }

    /**
     * Shows an Alert Dialog with parameters as title and description.
     * Also closes current activity, so this method should only be called when
     * closing is needed.
     *
     * @param title String
     * @param description String
     */
    public void showErrorDialog(String title, String description) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle(title);
        alertDialog.setMessage(description);
        alertDialog.setCancelable(true);
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                msgProcessor.finish();
            }
        });
        alertDialog.show();
    }

    public void stop() {
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
            mGatt = null;
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                scanLeDevice(false);
            }

        }
    }

    /**
     * Stops possible previous scan and starts a new one after SCAN_PERIOD ms
     * If "enable" parameter is false, scanning stops.
     *
     * @param enable boolean
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                    if (mGatt == null) {
                        showErrorDialog(context.getString(R.string.hm10_not_found_alert),
                                context.getString(R.string.hm10_not_found_description));
                    }
                }
            }, SCAN_PERIOD);
            mLEScanner.startScan(filters, settings, mScanCallback);
        } else {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    /**
     * Handles whatever is returned from BLE scanning
     */
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            //Check if HM10 is found, then establish connection
            BluetoothDevice btDevice = result.getDevice();
            if (btDevice.getAddress().equals(HM10_ADDRESS)) {
                connectToDevice(btDevice);
            }
        }

        /**
         * Called when more than one BT device are found
         *
         * @param results List<ScanResult>
         */
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
                //Check if HM10 is found, then establish connection
                if (sr.getDevice().getAddress().equals(HM10_ADDRESS)) {
                    connectToDevice(sr.getDevice());
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(context, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    /**
     * Handles connected GATT events.
     *
     *      onConnectionStateChange()
     *      onServicesDiscovered()
     *      onCharacteristicRead()
     *
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            //Get characteristic from which data wil be received or sent
            mCharacteristic =services.get(2).getCharacteristics().get(0);
            //enable notifications
            gatt.setCharacteristicNotification(mCharacteristic, true);

            if (doGetNodeList) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendMessage(PGNMessageProcessor.GET_NODE_LIST_MESSAGE);
                    }
                }, WAITING_TIME_GET_NODE_LIST);
                doGetNodeList = false;
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            //gatt.disconnect();
            Toast.makeText(context, characteristic.getValue().toString(), Toast.LENGTH_SHORT).show();
            msgProcessor.processReceivedMessage(characteristic.getValue());
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            System.out.println("onCharacteristicChanged => " +characteristic.getValue()[0]); //saca por la consola el campo "length"
            //Needs to run on UI Thread, otherwise UI won't refresh
            ((Activity) context).runOnUiThread(new Runnable() {
                public void run() {
                    Log.d("UI thread", "I am the UI thread");
                    msgProcessor.processReceivedMessage(characteristic.getValue());
                }
            });

        }
    };

    /**
     * Sends a byte buffer over the established BLE connection, by writing on a characteristic
     *
     * @param tx_buffer byte[]
     */
    public void sendMessage(byte[] tx_buffer) {
        if (mCharacteristic != null && ((mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) |
                (mCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mGatt != null) {
                mCharacteristic.setValue(tx_buffer);
                mGatt.writeCharacteristic(mCharacteristic);
            } else {
                showErrorDialog(context.getString(R.string.ble_not_connected_alert),
                                            context.getString(R.string.ble_not_connected_description));
            }

        }
    }


}
