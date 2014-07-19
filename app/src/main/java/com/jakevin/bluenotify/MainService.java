package com.jakevin.bluenotify;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by jakevin on 2014/7/17.
 */
public class MainService extends Service {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    private NotifyDataReceiver  receiver;
    private BlueToothReciver blueReceiver;
    // Name of the connected device
    private String mConnectedDeviceName = null;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    @Override
    public void onCreate() {
        super.onCreate();

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }else{

        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("Dx:", "onStartCommand.........");

        // Get the device MAC address
        String address = intent.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService = new BluetoothChatService(this, mHandler);
        mChatService.connect(device, true);

        //Register AccessibilityService
        receiver = new NotifyDataReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(".ConnectService");
        this.registerReceiver(receiver, filter);

        blueReceiver = new BlueToothReciver();
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(blueReceiver, filter3);
        Log.e("Dx:", "Broadcast registered.........");

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class BlueToothReciver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Device found
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Done searching
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                //Device is about to disconnect
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                pushToNotifi("Disconnected & disconnected");
                stopSelf();
                unregisterReceiver(blueReceiver);
                unregisterReceiver(receiver);
            }
        }


    }

    private class NotifyDataReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("Dx:", "Receiver got msg in onReceive()...");

            String notifyParcelable = intent.getStringExtra("NotifyData_Title");
            String contentString = intent.getStringExtra("NotifyData_CT");
            if (notifyParcelable != null) {
                sendMessage(contentString +"&"+notifyParcelable);
            }
        }
    }

    public void pushToNotifi(String msg){
        //在標題列上顯示
        Log.e("Dx:", ""+msg);

        String[] appmsg =msg.split("&");

        NotificationManager notificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.app_icon, "", System.currentTimeMillis());

        Intent it = new Intent();
        it.putExtra("notify_ID", msg);
        it.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |  Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 1, it, PendingIntent.FLAG_UPDATE_CURRENT);


        notification.ledARGB = 0xff00ff00;
        notification.ledOnMS = 300;
        notification.ledOffMS = 1000;
        notification.defaults |= Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND;
        long[] vibrate = {0,100,200,300};
        notification.vibrate = vibrate;
        notification.setLatestEventInfo(this, appmsg[0], appmsg[1], content);
        notificationManager.notify(1, notification);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            pushToNotifi("STATE CONNECTED & CONNECTED");
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            break;
                    }
                    break;

                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    pushToNotifi(readMessage);
                    break;

                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}
