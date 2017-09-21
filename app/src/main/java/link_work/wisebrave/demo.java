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
package link_work.wisebrave;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;

import link_work.wisebrave.BleMsg.BaseBleMessage;
import link_work.wisebrave.BleMsg.BleCmd03_getPower;
import link_work.wisebrave.BleMsg.BleCmd05_RemindOnOff;

public class demo extends Activity implements
        RadioGroup.OnCheckedChangeListener, OnItemSelectedListener {
    public static final String TAG = "nRFUART";
    private static final int UPDATE_MESSAGE = 1001;
    private static final int REQUEST_SELECT_DEVICE = 101;
    private static final int REQUEST_ENABLE_BT = 102;
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    public static demo hrDK;
    public static Activity mActivity;
    final int intf_none = 0;
    final int intf_ble_uart = 1;
    public Handler mHandler;
    public int iHR = 0;
    public int cur_HR = 0;
    protected PowerManager.WakeLock mWakeLock;
    int intf = intf_ble_uart;
    Config hr_config;
    TextView mTextView;
    TextView mTextView2;
    Button mButtonOnOff;
    Button mButtonRtOnOff;
    Button mButtonUnload;
    Button mButtonClear;
    Button mButtonScan;
    Button mButtonPower;
    TextView mTextPower;
    boolean bStartHRTest = false;
    boolean bStartHRTestOnOff = false;
    int iStableTime = 0;
    public ToggleButton mToggleButtonLose;
    public ToggleButton mToggleButtonSms;

    public ToggleButton mToggleButtonCall;
    int time_flag = 0;
    TextView mRemoteRssiVal;
    RadioGroup mRg;
    BleNotifyParse bleNotify = new BleNotifyParse();
    private Thread HRDThread;
    private Context context;
    private Handler handler = new Handler();
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mUartService = null;
    private BluetoothDevice mDevice = null;
    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            // *********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "UART_CONNECT_MSG");
                        mTextView.setText(mDevice.getName());
                        mTextView2.setText(R.string.connected);
                        mState = UART_PROFILE_CONNECTED;
                        if (!hr_config.isValid()) {
                            hr_config.save_config(mDevice.getName(), mDevice.getAddress());
                        }
                    }
                });
            }

            // *********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        mTextView2.setText(R.string.disconnect);
                        mState = UART_PROFILE_DISCONNECTED;
                        mUartService.close();
                        if (hr_config.isValid()) {
                            //mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                            //Log.d(TAG, "... onActivityResultdevice.address==" + mDevice
                            //		+ "mserviceValue" + mUartService);
                            //mTextView.setText(mDevice.getName());
                            mTextView2.setText(R.string.connecting);
                            //if (intf == intf_ble_uart)
                            mUartService.connect(mDevice.getAddress());
                        }
                        // setUiState();

                    }
                });
            }

            // *********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mUartService.enableTXNotification();
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//				BleCmd20_syncTime syncTime = new BleCmd20_syncTime();
//				setTx_data(syncTime.syncCurrentTime());

//				try {
//					Thread.currentThread().sleep(400);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}

//				BleCmd05_RemindOnOff remindOnOff = new BleCmd05_RemindOnOff();
//				setTx_data(remindOnOff.readRemindStatus(BleCmd05_RemindOnOff.REMIND_TYPE_LOST));

//				try {
//					Thread.currentThread().sleep(400);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}

//				setTx_data(remindOnOff.readRemindStatus(BleCmd05_RemindOnOff.REMIND_TYPE_SMS));

//				try {
//					Thread.currentThread().sleep(400);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				setTx_data(remindOnOff.readRemindStatus(BleCmd05_RemindOnOff.REMIND_TYPE_PHONE));

//				BleCmd03_getPower getPower = new BleCmd03_getPower();
//				setTx_data(getPower.getPower());
            }
            // *********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        String strText = "电量：";
                        strText += BaseBleMessage.byteArrHexToString(txValue);
                        mTextPower.setText(strText);
                    }
                });
                bleNotify.doParse(demo.this, txValue);
//				if (txValue[0] == 6) {
//					runOnUiThread(new Runnable() {
//						public void run() {
//							try {
//							} catch (Exception e) {
//								Log.e(TAG, e.toString());
//							}
//						}
//					});
//				}
            }
            // *********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT)) {
                //showMessage("Device doesn't support UART. Disconnecting");
                mUartService.disconnect();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mUartService.close();
                if (hr_config.isValid()) {
                    //mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    //Log.d(TAG, "... onActivityResultdevice.address==" + mDevice
                    //		+ "mserviceValue" + mUartService);
                    //mTextView.setText(mDevice.getName());
                    mTextView2.setText(R.string.connecting);
                    //if (intf == intf_ble_uart)
                    mUartService.connect(mDevice.getAddress());
                }
            }

        }
    };
    private BluetoothAdapter mBtAdapter = null;
    private ListView messageListView;
    private byte[] tx_data = new byte[512];
    private int tx_data_len = 0;
    private int tx_data_front = 0;
    private int tx_data_rear = 0;
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

                if (mUartService != null) {
                    mUartService.writeRXCharacteristic(send_buf);
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
    // UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder rawBinder) {
            if (intf == intf_ble_uart) {
                mUartService = ((UartService.LocalBinder) rawBinder)
                        .getService();
                Log.d(TAG, "onServiceConnected mService= " + mUartService);
                if (!mUartService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                } else {
                    if (hr_config.isValid()) {
                        if (mBtAdapter.isEnabled()) {
                            //mTextView2.setText("Connecting");
                            mUartService.connect(hr_config.getAddr());
                        }
                    }
                }
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            // // mService.disconnect(mDevice);
            if (intf == intf_ble_uart) {
                mUartService = null;
            }
        }
    };

    //注册广播,通过广播传递连接状态.
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);  //连接远程设备
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED); //断开与远程设备的GATT连接
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED); //搜索连接设备所支持的service
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT);
        return intentFilter;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) { //通过蓝牙管理器得到一个参考蓝牙适配器
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final Timer timer = new Timer(true);

        hr_config = new Config(demo.this);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available",
                    Toast.LENGTH_LONG).show();
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
        service_init();

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                "My Tag");
        this.mWakeLock.acquire();

        mTextView = findViewById(R.id.textHR);
        mTextView2 = findViewById(R.id.textHR2);
        mTextView.setTextColor(Color.RED);
        mTextView2.setTextColor(Color.GREEN);

        mButtonOnOff = findViewById(R.id.buttonOnOff);
        mButtonOnOff.setText(R.string.on);
        mButtonOnOff.setEnabled(false);

        mButtonRtOnOff = findViewById(R.id.buttonRtOnOff);
        mButtonRtOnOff.setText(R.string.rton);
        mButtonRtOnOff.setEnabled(false);

        mButtonUnload = findViewById(R.id.buttonUnload);
        mButtonUnload.setText(R.string.unload);
        mButtonUnload.setEnabled(false);

        mButtonClear = findViewById(R.id.buttonClear);
        mButtonClear.setText(R.string.clear);
        mButtonClear.setEnabled(false);

        mButtonScan = findViewById(R.id.buttonScan);
        mButtonScan.setText(R.string.start);

        mButtonPower = findViewById(R.id.button_power);
        mTextPower = findViewById(R.id.tv_power);

        if (hr_config.isValid()) {
            bStartHRTest = true;
            mButtonScan.setText(R.string.stop);
            mButtonOnOff.setEnabled(true);
            mButtonRtOnOff.setEnabled(true);
            mButtonUnload.setEnabled(true);
            mButtonClear.setEnabled(true);

            mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(hr_config.getAddr());

            Log.d(TAG, "... onActivityResultdevice.address==" + mDevice
                    + "mserviceValue" + mUartService);
            mTextView.setText(hr_config.getName());
            mTextView2.setText(R.string.connecting);
        }

        mToggleButtonLose = findViewById(R.id.toggleButtonLose);
        mToggleButtonSms = findViewById(R.id.toggleButtonSms);
        mToggleButtonCall = findViewById(R.id.toggleButtonCall);

        //mTextView.setText("ALG Ver : " + PXIALGMOTION.GetVersion());

        //achartengine_init();
        /*
         * Create a TextView and set its content. the text is retrieved by
		 * calling a native function.
		 */
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UPDATE_MESSAGE:
                        mTextView.setText("HR :" + iHR + "(" + cur_HR + ")");
                        break;
                }
                super.handleMessage(msg);
            }
        };

        // 获取手环当前的电量
        mButtonPower.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                BleCmd03_getPower getPower = new BleCmd03_getPower();
                setTx_data(getPower.getPower());
            }
        });

        mButtonOnOff.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        mButtonRtOnOff.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        mButtonUnload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        mButtonClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        mButtonScan.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                bStartHRTest = !bStartHRTest;
                if (bStartHRTest) {
                    mButtonScan.setText(R.string.stop);
                    mButtonOnOff.setEnabled(true);
                    mButtonRtOnOff.setEnabled(true);
                    mButtonUnload.setEnabled(true);
                    mButtonClear.setEnabled(true);
                    Intent newIntent = new Intent(demo.this, DeviceListActivity.class);
                    startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                } else {
                    // Disconnect button pressed
                    mButtonOnOff.setEnabled(false);
                    mButtonRtOnOff.setEnabled(false);
                    mButtonUnload.setEnabled(false);
                    mButtonClear.setEnabled(false);
                    mButtonScan.setText(R.string.start);
                    hr_config.clear_config();
                    //mTextView2.setText("");
                }

                if (intf == intf_ble_uart) {

                    if (bStartHRTest) {
                        // Connect button pressed, open DeviceListActivity
                        // class, with popup windows that scan for devices

                        try {

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Disconnect button pressed
                        try {

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (mDevice != null) {
                            mUartService.disconnect();
                        }

                    }

                }
            }

        });

        mToggleButtonLose.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean status = false;

                if (mToggleButtonLose.isChecked()) {
                    status = true;
                }
                mToggleButtonLose.setChecked(status);
                BleCmd05_RemindOnOff ctrlCmd = new BleCmd05_RemindOnOff();
                setTx_data(ctrlCmd.switchRemind(BleCmd05_RemindOnOff.REMIND_TYPE_LOST, status));
            }
        });

        mToggleButtonSms.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean status = false;
                if (mToggleButtonSms.isChecked()) {
                    status = true;
                }
                mToggleButtonSms.setChecked(status);
                BleCmd05_RemindOnOff ctrlCmd = new BleCmd05_RemindOnOff();
                setTx_data(ctrlCmd.switchRemind(BleCmd05_RemindOnOff.REMIND_TYPE_SMS, status));
            }
        });

        mToggleButtonCall.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean status = false;
                if (mToggleButtonCall.isChecked()) {
                    status = true;
                }
                mToggleButtonCall.setChecked(status);
                BleCmd05_RemindOnOff ctrlCmd = new BleCmd05_RemindOnOff();
                setTx_data(ctrlCmd.switchRemind(BleCmd05_RemindOnOff.REMIND_TYPE_PHONE, status));
            }
        });

        hrDK = this;
        mActivity = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
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

    public void setTx_data(byte[] tx_data) {
        if (tx_data == null) {
            return;
        }

        if (mUartService == null) {
            return;
        }

        if (!mUartService.isConnected()) {
            return;
        }

        for (byte aTx_data : tx_data) {
            if (tx_data_len >= 512) {
                tx_data_rear = (tx_data_rear + 1) % 512;
                tx_data_len--;
            }
            this.tx_data[tx_data_front] = aTx_data;
            tx_data_front = (tx_data_front + 1) % 512;
            tx_data_len++;
        }
        if (time_flag == 0) {
            handler.postDelayed(runnable, 200);
            time_flag = 1;
        }
    }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        this.mWakeLock.release();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(
                    UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }

        try {
            unbindService(mServiceConnection);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        if (mUartService != null) {
            mUartService.stopSelf();
            mUartService = null;
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

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
                            + "mserviceValue" + mUartService);
                    mTextView.setText(mDevice.getName());
                    mTextView2.setText(R.string.connecting);
                    if (intf == intf_ble_uart)
                        mUartService.connect(deviceAddress);

                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ",
                            Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ",
                            Toast.LENGTH_SHORT).show();
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

    public class timerTask extends TimerTask {
        public void run() {

        }
    }
}
