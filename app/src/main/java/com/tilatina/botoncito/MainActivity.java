package com.tilatina.botoncito;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.VIBRATE;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BLE = 1968;
    private static final int PERMISSION_REQUEST_CODE = 1970;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private LeDeviceListAdapter mLeDeviceListAdapter;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 120000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!checkPermission()) {
            requestPermission();
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE No soportado", Toast.LENGTH_SHORT).show();
            finish();
        }

        mLeDeviceListAdapter = new LeDeviceListAdapter();

        ListView deviceList = (ListView)findViewById(R.id.device_list);
        deviceList.setAdapter(mLeDeviceListAdapter);

        SwipeRefreshLayout swipe = (SwipeRefreshLayout)findViewById(R.id.swipe);
        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
            }
        });

        BluetoothManager bleManager = (BluetoothManager)
                getSystemService(Service.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bleManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BLE);
        } else {
            scanLeDevice(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BLE:
                Log.d("SOME", String.format("BLE regresó estado. Estado = %s", resultCode));
                if (resultCode == BluetoothAdapter.STATE_ON) {
                    scanLeDevice(true);
                }
                break;
        }
    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
            Log.d("onLeScan", String.format("bluetoothDevice rssi = %s", rssi));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(bluetoothDevice);
                    mLeDeviceListAdapter.notifyDataSetChanged();

                }
            });
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler = new Handler();
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    Log.d("Botoncito", "Tiempo vencido. Terminando scan");
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    SwipeRefreshLayout swipe = (SwipeRefreshLayout)findViewById(R.id.swipe);
                    swipe.setRefreshing(false);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            Log.d("Botoncito", "comenzando scan");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            Log.d("Botoncito", "Deshabilitado. Terminando scan");
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            SwipeRefreshLayout swipe = (SwipeRefreshLayout)findViewById(R.id.swipe);
            swipe.setRefreshing(false);
        }
    }

    /**
     * Revisa que la aplicacion tenga permisos de localización
     * @return regresa si tiene los permisos o no
     */
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                ACCESS_FINE_LOCATION);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                ACCESS_COARSE_LOCATION);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(),
                VIBRATE);

        Log.i("Common.LOG_TAG", String.format("Permisos: ACCESS_FINE_LOCATION = %s, ACCESS_COARSE_LOCATION = %s, VIBRATE = %s ",
                result, result1, result2));
        return result == PackageManager.PERMISSION_GRANTED
                && result1 == PackageManager.PERMISSION_GRANTED
                && result2 == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Si no tiene los permisos lo pide para el acceso a la localización
     */
    private void requestPermission() {

        Log.d("Common.LOG_TAG", "requestPermission");
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION,
                VIBRATE}, PERMISSION_REQUEST_CODE);
        //recreate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults){
        for (int i = 0; i < grantResults.length; i++) {
            Log.d("Common.LOG_TAG", String.format("grantResult[%s] = %s", i, grantResults[i]));
        }
        switch (requestCode){
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length == 3) {

                    boolean fineLocAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean coarseLocAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean vibrateAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED;

                    Log.d("Common.LOG_TAG", String.format("onRequestPermissionsResult: fineLocAccepted = %s, coarseLocAcceped = %s, vibrateAccepted = %s",
                            fineLocAccepted, coarseLocAccepted, vibrateAccepted));

                    if (!fineLocAccepted && !coarseLocAccepted && !vibrateAccepted){
                        showSnackBar();
                    } else {
                        recreate();
                    }
                } else {
                    Log.e("Common.LOG_TAG", "onRequestPermissionsResult no entrega 3 resultados");
                }
        }
    }

    private void showSnackBar() {

      Toast.makeText(this, "snackbar", Toast.LENGTH_LONG).show();
    }

    public void openSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }
        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }
        public void clear() {
            mLeDevices.clear();
        }
        @Override
        public int getCount() {
            return mLeDevices.size();
        }
        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
