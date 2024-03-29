/******************************************************************************
 * Copyright (C) Cambridge Silicon Radio Limited 2015
 * <p>
 * This software is provided to the customer for evaluation
 * purposes only and, as such early feedback on performance and operation
 * is anticipated. The software source code is subject to change and
 * not intended for production. Use of developmental release software is
 * at the user's own risk. This software is provided "as is," and CSR
 * cautions users to determine for themselves the suitability of using the
 * beta release version of this software. CSR makes no warranty or
 * representation whatsoever of merchantability or fitness of the product
 * for any particular purpose or use. In no event shall CSR be liable for
 * any consequential, incidental or special damages whatsoever arising out
 * of the use of or inability to use this software, even if the user has
 * advised CSR of the possibility of such damages.
 ******************************************************************************/

package com.csr.masterapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * Activity used to scan for bridge devices. Show the results in a list. Automatically select the devices with the
 * smallest RSSI and pass them to the MainActivity for connection.
 */
public class ScanResultsActivity extends Activity implements AdapterView.OnItemClickListener {
    private static final int REQUEST_ENABLE_BT = 1;

    // Adjust this value to control how long scan should last for. Higher values will drain the battery more.
    // Adjust this value in the derived class.
    protected long mScanPeriodMillis = 5000;

    ListView mScanListView = null;

    private static ArrayList<ScanInfo> mScanResults = new ArrayList<ScanInfo>();

    private static HashSet<String> mScanAddreses = new HashSet<String>();

    private static ScanResultsAdapter mScanResultsAdapter;

    private static Resources mResoruces;

    private BluetoothAdapter mBtAdapter = null;

    private static Handler mHandler = new Handler();

    private Button mScanButton = null;

    private boolean mCheckBt = false;

    private static final int INDEX_UUID_1 = 5;
    private static final int INDEX_UUID_2 = 6;
    private static final byte UUID_1 = (byte) 0xF1;
    private static final byte UUID_2 = (byte) 0xFE;

    private static final String TAG = "ScanResultsActivity";

    // number of connection
    private int scanAttempts = 0;

    /**
     * Handle Bluetooth connection when the user selects a device.
     * @TODO
     */
    protected void connectBluetooth(int i) {
        Intent intent = new Intent(this, MainActivity.class);
        // Try top 3 devices
        ArrayList<BluetoothDevice> devices = new ArrayList<>();
//        for (int i = 0; i < mScanResults.size() && i < 3; i++) {
        ScanInfo info = mScanResults.get(i);
        devices.add(mBtAdapter.getRemoteDevice(info.address));          //getRemoteDevice()返回相应的被指定蓝牙连接的远端设备。
//       }
        intent.putParcelableArrayListExtra(BluetoothDevice.EXTRA_DEVICE, devices);
        Log.i(TAG, "Devices : " + intent.putParcelableArrayListExtra(BluetoothDevice.EXTRA_DEVICE, devices));

        this.startActivity(intent);
    }

    //    触发选择连接蓝牙
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        connectBluetooth(position);
    }


    /**
     * When the activity is created, set up the UI and start scanning.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Prevent screen rotation.
//        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        // Add animated progress indicator in top right corner.
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        mResoruces = this.getResources();

        setContentView(R.layout.activity_scan_results);
        mScanListView = (ListView) this.findViewById(R.id.scanListView);
        mScanResultsAdapter = new ScanResultsAdapter(this, mScanResults);
        mScanListView.setAdapter(mScanResultsAdapter);
        mScanListView.setOnItemClickListener(this);
        //重新搜索按钮觸發
        mScanButton = (Button) findViewById(R.id.buttonScan);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });

        final BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = btManager.getAdapter();

        // Register for broadcasts on BluetoothAdapter state change so that we can tell if it has been turned off.
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(mReceiver, filter);
        checkEnableBt();

        if (mBtAdapter.isEnabled()) {
            Log.i(TAG, "是否打开蓝牙适配器 ：" + mBtAdapter.isEnabled());
            scanLeDevice(true);
        }
    }

    /**
     * When the Activity is resumed, clear the scan results list.
     */
    @Override
    protected void onResume() {
        super.onResume();
        clearScanResults();
        if (mCheckBt) {
            checkEnableBt();
            Log.d(TAG, "onResume: 搜寻ACTIVITY回到后台！！");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        // Set flag to check Bluetooth is still enabled when we are resumed.
        // If we end up being destroyed this flag's state will be forgotten, but that's fine because then
        // onCreate will perform the Bluetooth check anyway.
        mCheckBt = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }


    /**
     *  Display a dialogue requesting Bluetooth to be enabled if it isn't already.
     */
    private void checkEnableBt() {
        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * Callback activated after the user responds to the enable Bluetooth dialogue.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCheckBt = false;
        if (requestCode == REQUEST_ENABLE_BT && resultCode != RESULT_OK) {
//            mScanButton.setEnabled(false);
            Toast.makeText(this, getString(R.string.bluetooth_not_enabled), Toast.LENGTH_LONG).show();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    Toast.makeText(context, getString(R.string.bluetooth_disabled), Toast.LENGTH_LONG).show();
                    scanLeDevice(false);
                    clearScanResults();
//                    mScanButton.setEnabled(false);
                } else if (state == BluetoothAdapter.STATE_ON) {
                    Toast.makeText(context, getString(R.string.bluetooth_enabled), Toast.LENGTH_LONG).show();
//                    mScanButton.setEnabled(true);
                    mBtAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
                }
            }
        }
    };


    /**
     * Clear the cached scan results, and update the display.
     */
    private void clearScanResults() {
        mScanResults.clear();
        mScanAddreses.clear();
        // Make sure the display is updated; any old devices are removed from the ListView. 
        mScanResultsAdapter.notifyDataSetChanged();
    }


    private Runnable scanTimeout = new Runnable() {
        @Override
        public void run() {
            mBtAdapter.stopLeScan(mLeScanCallback);
            setProgressBarIndeterminateVisibility(false);
            // Connect to the device with the smallest RSSI value
            if (!mScanResults.isEmpty()) {
                scanAttempts = 0;
//	            connectBluetooth();
            } else {
                Toast.makeText(getApplicationContext(), mResoruces.getString(R.string.not_discove_bridge_device), Toast.LENGTH_LONG).show();
//            	mScanButton.setEnabled(true);
                scanAttempts++;
                if (scanAttempts >= 2) {
                    //show an alert asking to reset the Bluetooth
                    askResetBluetooth();
                }
            }
        }
    };

    /**
     * Pop up an alert asking if the user want to reset the bluetooth
     */
    private void askResetBluetooth() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ScanResultsActivity.this);

        alertDialogBuilder.setTitle(R.string.reset_bluetooth);
        alertDialogBuilder.setMessage(R.string.prompt_not_discove_bridge_device_can_reset_buletooth);
        // set positive button: Yes message
        alertDialogBuilder.setPositiveButton(R.string.reset, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetBluetooth();
                dialog.cancel();
            }
        });
        // set negative button: No message
        alertDialogBuilder.setNegativeButton(R.string.later_handle, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // cancel the alert box and put a Toast to the user
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        // show alert
        alertDialog.show();
    }

    /**
     * Stop and Start the Bluetooth. We shoudn't use this method without user permission.
     */
    private void resetBluetooth() {
        if (mBtAdapter != null) {
            mBtAdapter.disable();
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    checkEnableBt();
                }
            }, 200);

        }
    }

    /**
     * Start or stop scanning. Only scan for a limited amount of time defined by SCAN_PERIOD.
     *
     * @param enable
     *            Set to true to enable scanning, false to stop.
     */
    private void scanLeDevice(final boolean enable) {

        if (enable) {
            // Stops scanning after a predefined scan period.
            mHandler.postDelayed(scanTimeout, mScanPeriodMillis);
            clearScanResults();     //清除搜索结果
            setProgressBarIndeterminateVisibility(true);
            mBtAdapter.startLeScan(mLeScanCallback);
//            mScanButton.setEnabled(false);
        } else {
            // Cancel the scan timeout callback if still active or else it may fire later.
            mHandler.removeCallbacks(scanTimeout);
            setProgressBarIndeterminateVisibility(false);
            mBtAdapter.stopLeScan(mLeScanCallback);
//            mScanButton.setEnabled(true);
        }
    }

    /**
     * Callback for scan results.
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (device.getName() == null) {
                        // Sometimes devices are seen with a null name and empirically connection
                        // to such devices is less reliable, so ignore them.
                        return;
                    }
                    // Check that this isn't a device we have already seen, and add it to the list.
                    if (!mScanAddreses.contains(device.getAddress())) {
                        if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE &&
                                scanRecord[INDEX_UUID_1] == UUID_1 && scanRecord[INDEX_UUID_2] == UUID_2) {
                            ScanInfo scanResult = new ScanInfo(device.getName(), device.getAddress(), rssi);
                            mScanAddreses.add(device.getAddress());
                            mScanResults.add(scanResult);
                            Collections.sort(mScanResults);
                            mScanResultsAdapter.notifyDataSetChanged();
                        }
                    } else {
                        for (ScanInfo info : mScanResults) {
                            if (info.address.equalsIgnoreCase((device.getAddress()))) {
                                info.rssi = rssi;
                                Collections.sort(mScanResults);
                                mScanResultsAdapter.notifyDataSetChanged();
                                break;
                            }
                        }
                    }
                }
            });
        }
    };

    /**
     * The adapter that allows the contents of ScanInfo objects to be displayed in the ListView. The device name,
     * address, RSSI and the icon specified in appearances.xml are displayed.
     */
    private class ScanResultsAdapter extends BaseAdapter {
        private Activity activity;
        private ArrayList<ScanInfo> data;
        private LayoutInflater inflater = null;

        public ScanResultsAdapter(Activity a, ArrayList<ScanInfo> object) {
            activity = a;
            data = object;
            inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return data.size();
        }

        public Object getItem(int position) {
            return data.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            if (convertView == null)
                vi = inflater.inflate(R.layout.scan_list_row, null);

            TextView nameText = (TextView) vi.findViewById(R.id.name);
            TextView addressText = (TextView) vi.findViewById(R.id.address);
            TextView rssiText = (TextView) vi.findViewById(R.id.rssi);

            ScanInfo info = (ScanInfo) data.get(position);
            nameText.setText(info.name);
            addressText.setText(info.address);
            if (info.rssi != 0) {
                rssiText.setText(String.valueOf(info.rssi) + "dBm");
            }
            return vi;
        }

        @Override
        public void notifyDataSetChanged() {
            // before notify. sort the data by RSSI.
            Collections.sort(data);

            super.notifyDataSetChanged();
        }
    }

    private class ScanInfo implements Comparable<ScanInfo> {
        public String name;
        public String address;
        public int rssi;

        public ScanInfo(String name, String address, int rssi) {
            this.name = name;
            this.address = address;
            this.rssi = rssi;
        }

        @Override
        public int compareTo(ScanInfo another) {
            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if (rssi == another.rssi) return EQUAL;
            if (this.rssi < another.rssi) return AFTER;
            if (this.rssi > another.rssi) return BEFORE;

            return EQUAL;
        }
    }
}
