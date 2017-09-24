/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package link_work.wisebrave.Activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import link_work.wisebrave.Bean.UARTBean;
import link_work.wisebrave.BleMsg.BaseBleMessage;
import link_work.wisebrave.BleMsg.BleCmd03_getPower;
import link_work.wisebrave.BleMsg.BleCmd05_RemindOnOff;
import link_work.wisebrave.BleMsg.BleCmd06_getData;
import link_work.wisebrave.R;
import link_work.wisebrave.Service.UARTService;
import link_work.wisebrave.Service.UARTStatusChangeReceiver;
import link_work.wisebrave.Util.BleNotifyParse;
import link_work.wisebrave.Util.Config;

public class MainActivity extends Activity implements
        RadioGroup.OnCheckedChangeListener, OnItemSelectedListener {
    public static final String TAG = "nRFUART";
    private static final int UPDATE_MESSAGE = 1001;
    private static final int REQUEST_SELECT_DEVICE = 101;
    private static final int REQUEST_ENABLE_BT = 102;
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    public static MainActivity hrDK;
    public static Activity mActivity;
    final int intf_ble_uart = 1;
    public int iHR = 0;
    public int cur_HR = 0;
    @BindView(R.id.toggleButtonLose)
    public ToggleButton mToggleButtonLose;
    @BindView(R.id.toggleButtonSms)
    public ToggleButton mToggleButtonSms;
    @BindView(R.id.toggleButtonCall)
    public ToggleButton mToggleButtonCall;
    protected PowerManager.WakeLock mWakeLock;
    int intf = intf_ble_uart;
    Config hr_config;
    @BindView(R.id.device_name)
    TextView deviceName;
    @BindView(R.id.connect_status)
    TextView connectStatus;
    @BindView(R.id.buttonScan)
    Button mButtonScan;
    @BindView(R.id.tv_power)
    TextView mTextPower;
    @BindView(R.id.button_power)
    Button mButtonPower;
    @BindView(R.id.tv_heart_rate)
    TextView heartRate;
    @BindView(R.id.button_heart_rate)
    Button heartBtn;
    boolean bStartHRTest = false;
    int time_flag = 0;
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
            if (intf == intf_ble_uart) {
                mUARTService = ((UARTService.LocalBinder) rawBinder).getService();
                Log.d(TAG, "onServiceConnected mService= " + mUARTService);
                if (!mUARTService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
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
            if (intf == intf_ble_uart) {
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

    @Override
    public void onCreate(Bundle savedInstanceState) { // 通过蓝牙管理器得到一个参考蓝牙适配器
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ButterKnife.bind(this);

        hr_config = new Config(MainActivity.this);
        service_init();

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            showMessage("Bluetooth is not available");
            finish();
            return;
        }
        if (hr_config.isValid()) {
            if (!mBtAdapter.isEnabled()) {
                mBtAdapter.enable();
            }
        }
//		if (!mBtAdapter.isEnabled()) {
//			Log.i(TAG, "onClick - BT not enabled yet");
//			Intent enableIntent = new Intent(
//					BluetoothAdapter.ACTION_REQUEST_ENABLE);
//			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
//		}

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();


        if (hr_config.isValid()) {
            bStartHRTest = true;
            mButtonScan.setText(R.string.stop);

            mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(hr_config.getAddress());

            Log.d(TAG, "... onActivityResultdevice.address==" + mDevice
                    + "mserviceValue" + mUARTService);
            deviceName.setText(hr_config.getName());
            connectStatus.setText(R.string.connecting);
        }

        hrDK = this;
        mActivity = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (intf == intf_ble_uart) {
            if (mBtAdapter != null) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onResume - BT not enabled yet");
                    Intent enableIntent = new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
            }
        }
    }

    // *******************BLE**************************************//
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }

    /*
    * 初始化广播服务
    * */
    private void service_init() {
        Intent bindIntent = new Intent(this, UARTService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                uartStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mWakeLock.release();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(uartStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }

        try {
            unbindService(mServiceConnection);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        if (mUARTService != null) {
            mUARTService.stopSelf();
            mUARTService = null;
        }
    }

    /*
    * 显示提示信息
    * */
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                // When the DeviceListActivity return, with the selected device
                // address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice
                            + "mserviceValue" + mUARTService);
                    deviceName.setText(mDevice.getName());
                    connectStatus.setText(R.string.connecting);
                    if (intf == intf_ble_uart)
                        mUARTService.connect(deviceAddress);

                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    showMessage("Bluetooth has turned on ");
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    showMessage("Problem in BT Turning ON ");
                    //finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {

    }

    @OnClick(R.id.button_heart_rate)
    void testHeard() {
        BleCmd06_getData getData = new BleCmd06_getData();
        getData.onHR();
    }

    @OnClick(R.id.toggleButtonLose)
    void antiLose() {
        boolean status = false;

        if (mToggleButtonLose.isChecked()) {
            status = true;
        }
        mToggleButtonLose.setChecked(status);
        BleCmd05_RemindOnOff ctrlCmd = new BleCmd05_RemindOnOff();
        // 防丢提醒
        initSendData(ctrlCmd.switchRemind(BleCmd05_RemindOnOff.REMIND_TYPE_LOST, status));
    }

    @OnClick(R.id.toggleButtonSms)
    void notifySms() {
        boolean status = false;
        if (mToggleButtonSms.isChecked()) {
            status = true;
        }
        mToggleButtonSms.setChecked(status);
        BleCmd05_RemindOnOff ctrlCmd = new BleCmd05_RemindOnOff();
        initSendData(ctrlCmd.switchRemind(BleCmd05_RemindOnOff.REMIND_TYPE_SMS, status));
    }

    @OnClick(R.id.toggleButtonCall)
    void notifyPhone() {
        boolean status = false;
        if (mToggleButtonCall.isChecked()) {
            status = true;
        }
        mToggleButtonCall.setChecked(status);
        BleCmd05_RemindOnOff ctrlCmd = new BleCmd05_RemindOnOff();
        initSendData(ctrlCmd.switchRemind(BleCmd05_RemindOnOff.REMIND_TYPE_PHONE, status));
    }

    @OnClick(R.id.button_power)
    void showPower() {
        BleCmd03_getPower getPower = new BleCmd03_getPower();
        Log.i("test", "showPower: " + Arrays.toString(getPower.getPower()));
        initSendData(getPower.getPower());
    }

    @OnClick(R.id.buttonScan)
    void btnScan() {
        bStartHRTest = !bStartHRTest;
        if (bStartHRTest) {
            mButtonScan.setText(R.string.stop);
            Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
        } else {
            // Disconnect button pressed
            mButtonScan.setText(R.string.start);
            hr_config.clear_config();
            connectStatus.setText("");
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

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(UARTBean event) {
        String action = event.getAction();
        // *********连接动作发生的时候************//
        if (action.equals(UARTService.ACTION_GATT_CONNECTED)) {
            Log.d(TAG, "UART_CONNECT_MSG");
            deviceName.setText(mDevice.getName());
            connectStatus.setText(R.string.connected);
            if (!hr_config.isValid()) {
                hr_config.save_config(mDevice.getName(), mDevice.getAddress());
            }
        }

        // **********当设备断开连接的时候***********//
        if (action.equals(UARTService.ACTION_GATT_DISCONNECTED)) {
            Log.d(TAG, "UART_DISCONNECT_MSG");
            connectStatus.setText(R.string.disconnect);
            mUARTService.close();
            if (hr_config.isValid()) {
                connectStatus.setText(R.string.connecting);
                // 如果intf等于原来的intf的话，就重新连接
                if (intf == intf_ble_uart) {
                    mUARTService.connect(mDevice.getAddress());
                }
            }
        }

        // **********当扫描到设备的时候***********//
        if (action.equals(UARTService.ACTION_GATT_SERVICES_DISCOVERED)) {
            mUARTService.enableTXNotification();
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // ***********当数据可用的时候**********//
        if (action.equals(UARTService.ACTION_DATA_AVAILABLE)) {

            final byte[] txValue = event.getData();
            String strText = "电量：";
            Log.e("test", Arrays.toString(txValue));
            strText += BaseBleMessage.byteArrHexToString(txValue);
            Log.e("test", strText);
            mTextPower.setText(strText);
            BleNotifyParse.getInstance().doParse(MainActivity.this, txValue);
        }
        // ***********如果设备不支持的话**********//
        if (action.equals(UARTService.DEVICE_DOES_NOT_SUPPORT)) {
            showMessage("Device doesn't support UART. Disconnecting");
            mUARTService.disconnect();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mUARTService.close();
            if (hr_config.isValid()) {
                connectStatus.setText(R.string.connecting);
                if (intf == intf_ble_uart) {
                    mUARTService.connect(mDevice.getAddress());
                }
            }
        }
    }
}
