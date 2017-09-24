package link_work.wisebrave.Util;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import link_work.wisebrave.Service.UARTService;
import link_work.wisebrave.Service.UARTStatusChangeReceiver;

/**
 * Created by wangyu on 17-9-24.
 */

public class HandleData {
    public static final int UPDATE_MESSAGE = 1001;
    public static final int REQUEST_SELECT_DEVICE = 101;
    public static final int REQUEST_ENABLE_BT = 102;
    public static final int UART_PROFILE_READY = 10;
    public static final int UART_PROFILE_CONNECTED = 20;
    public static final int UART_PROFILE_DISCONNECTED = 21;
    public static final int STATE_OFF = 10;
    private static final String TAG = "nRFUART";
    private static HandleData handleData;
    private final int intFlag_ble_uart = 1;
    public int iHR = 0;
    public int cur_HR = 0;
    private PowerManager.WakeLock mWakeLock;
    private int intFlag = intFlag_ble_uart;
    private Config hr_config;
    private boolean bStartHRTest = false;
    private int time_flag = 0;
    private byte[] tx_data = new byte[512];
    private int tx_data_len = 0;
    private int tx_data_front = 0;
    private int tx_data_rear = 0;
    private UARTService mUARTService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    /**
     * UART service connected/disconnected
     * 通用异步收发传输器， 一对一，以位为单位发送。
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder rawBinder) {
            if (intFlag == intFlag_ble_uart) {
                mUARTService = ((UARTService.LocalBinder) rawBinder).getService();
                Log.d(TAG, "onServiceConnected mService= " + mUARTService);
                if (!mUARTService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                } else {
                    if (hr_config.isValid()) {
                        if (mBtAdapter.isEnabled()) {
                            mUARTService.connect(hr_config.getAddress());
                        }
                    }
                }
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            if (intFlag == intFlag_ble_uart) {
                mUARTService = null;
            }
        }
    };
    private Handler handler = new Handler();
    // 发送数据并处理数据
    private Runnable runnable = new Runnable() {
        public void run() {
            if (tx_data_len > 0) {
                int len;
                if (tx_data_len > 20) {
                    len = 20;
                } else {
                    len = tx_data_len;
                }

                byte[] send_buf = new byte[len];
                for (int i = 0; i < len; i++) {
                    send_buf[i] = tx_data[tx_data_rear];
                    tx_data_rear = (tx_data_rear + 1) % 512;
                }

                if (mUARTService != null) {
                    mUARTService.writeRXCharacteristic(send_buf);
                }
                tx_data_len = tx_data_len - len;
            }

            if (tx_data_len > 0) {
                handler.postDelayed(this, 200);
            } else {
                time_flag = 0;
            }
        }
    };
    private UARTStatusChangeReceiver uartStatusChangeReceiver = new UARTStatusChangeReceiver();

    private HandleData(Context context, Activity activity) {

        hr_config = new Config(context);
        service_init(activity);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            return;
        }
        if (hr_config.isValid()) {
            if (!mBtAdapter.isEnabled()) {
                mBtAdapter.enable();
            }
        }

        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();


        if (hr_config.isValid()) {
            bStartHRTest = true;
            mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(hr_config.getAddress());
            Log.d(TAG, "... onActivityResultdevice.address==" + mDevice
                    + "mserviceValue" + mUARTService);
        }
    }

    /*
    * 单例模式
    * */
    public static HandleData getInstance(Context context, Activity activity) {
        if (handleData == null) {
            handleData = new HandleData(context, activity);
        }
        return handleData;
    }

    /**
     * 注册广播,通过广播传递连接状态.
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UARTService.ACTION_GATT_CONNECTED);  //连接远程设备
        intentFilter.addAction(UARTService.ACTION_GATT_DISCONNECTED); //断开与远程设备的GATT连接
        intentFilter.addAction(UARTService.ACTION_GATT_SERVICES_DISCOVERED); //搜索连接设备所支持的service
        intentFilter.addAction(UARTService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UARTService.DEVICE_DOES_NOT_SUPPORT);
        return intentFilter;
    }

    public UARTService getmUARTService() {
        return mUARTService;
    }

    public BluetoothDevice getmDevice() {
        return mDevice;
    }

    public void onResume(Activity activity) {
        if (intFlag == intFlag_ble_uart) {
            if (mBtAdapter != null) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onResume - BT not enabled yet");
                    Intent enableIntent = new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    activity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
            }
        }
    }

    public void onDestroy(Activity activity) {
        this.mWakeLock.release();
        try {
            LocalBroadcastManager.getInstance(activity).unregisterReceiver(uartStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }

        try {
            activity.unbindService(mServiceConnection);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        if (mUARTService != null) {
            mUARTService.stopSelf();
            mUARTService = null;
        }
    }

    /*
    * 初始化广播服务
    * */
    private void service_init(Activity activity) {
        Intent bindIntent = new Intent(activity, UARTService.class);
        activity.bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(activity).registerReceiver(
                uartStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    public void activityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                // When the DeviceListActivity return, with the selected device
                // address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
//                    deviceName.setText(mDevice.getName());
//                    connectStatus.setText(R.string.connecting);
                    if (intFlag == intFlag_ble_uart)
                        mUARTService.connect(deviceAddress);

                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
//                    showMessage("Bluetooth has turned on ");
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
//                    showMessage("Problem in BT Turning ON ");
                    //finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    /**
     * 生成底层数据包，发送给手环。
     *
     * @param tx_data 底层数据包
     */
    public void initSendData(byte[] tx_data) {
        Log.i("test", "initSendData 进来了");
        if (tx_data == null) {
            return;
        }
        if (mUARTService == null) {
            return;
        }

        if (!mUARTService.isConnected()) {
            return;
        }
        Log.i("test", "initSendData: -> 底层数据包的长度为（默认为6个）" + tx_data.length);
        for (byte aTx_data : tx_data) {
            if (tx_data_len >= 512) {
                tx_data_rear = (tx_data_rear + 1) % 512;
                tx_data_len--;
            }
            this.tx_data[tx_data_front] = aTx_data;
            tx_data_front = (tx_data_front + 1) % 512;
            tx_data_len++;
        }
        Log.i("test", "initSendData: tx_data_rear->" + tx_data_rear);
        if (time_flag == 0) {
            handler.postDelayed(runnable, 200);
            time_flag = 1;
        }
        Log.i("test", "initSendData 出来了");
    }

    public void clearConfig() {
        hr_config.clear_config();
    }

    public void saveConfig() {
        if (!isConfigValid()) {
            hr_config.save_config(mDevice.getName(), mDevice.getAddress());
        }
    }

    public boolean isConfigValid() {
        return hr_config.isValid();
    }

    /**
     * 直接重连
     */
    public void reconnectI() {
        if (hr_config.isValid()) {
            // 如果intf等于原来的intf的话，就重新连接
            if (intFlag == intFlag_ble_uart) {
                mUARTService.connect(mDevice.getAddress());
            }
        }
    }

    public void reconnect() {
        if (intFlag == intFlag_ble_uart) {
            mUARTService.connect(mDevice.getAddress());
        }
    }

    public boolean isbStartHRTest() {
        return bStartHRTest;
    }

    public void setbStartHRTest(boolean bStartHRTest) {
        this.bStartHRTest = bStartHRTest;
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
