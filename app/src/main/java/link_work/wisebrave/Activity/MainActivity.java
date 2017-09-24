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
import android.content.Intent;
import android.os.Bundle;
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
import link_work.wisebrave.Util.BleNotifyParse;
import link_work.wisebrave.Util.HandleData;

public class MainActivity extends Activity implements
        RadioGroup.OnCheckedChangeListener, OnItemSelectedListener {
    public static MainActivity hrDK;
    public static Activity mActivity;
    @BindView(R.id.toggleButtonLose)
    public ToggleButton mToggleButtonLose;
    @BindView(R.id.toggleButtonSms)
    public ToggleButton mToggleButtonSms;
    @BindView(R.id.toggleButtonCall)
    public ToggleButton mToggleButtonCall;
    public HandleData handleData;
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

    @Override
    public void onCreate(Bundle savedInstanceState) { // 通过蓝牙管理器得到一个参考蓝牙适配器
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ButterKnife.bind(this);

        handleData = HandleData.getInstance(this, this);

        hrDK = this;
        mActivity = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleData.onResume(this);
    }

    // *******************BLE**************************************//
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handleData.onDestroy(this);
    }

    /*
    * 显示提示信息
    * */
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        handleData.activityResult(requestCode, resultCode, data);
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
        handleData.initSendData(ctrlCmd.switchRemind(BleCmd05_RemindOnOff.REMIND_TYPE_LOST, status));
    }

    @OnClick(R.id.toggleButtonSms)
    void notifySms() {
        boolean status = false;
        if (mToggleButtonSms.isChecked()) {
            status = true;
        }
        mToggleButtonSms.setChecked(status);
        BleCmd05_RemindOnOff ctrlCmd = new BleCmd05_RemindOnOff();
        handleData.initSendData(ctrlCmd.switchRemind(BleCmd05_RemindOnOff.REMIND_TYPE_SMS, status));
    }

    @OnClick(R.id.toggleButtonCall)
    void notifyPhone() {
        boolean status = false;
        if (mToggleButtonCall.isChecked()) {
            status = true;
        }
        mToggleButtonCall.setChecked(status);
        BleCmd05_RemindOnOff ctrlCmd = new BleCmd05_RemindOnOff();
        handleData.initSendData(ctrlCmd.switchRemind(BleCmd05_RemindOnOff.REMIND_TYPE_PHONE, status));
    }

    @OnClick(R.id.button_power)
    void showPower() {
        BleCmd03_getPower getPower = new BleCmd03_getPower();
        Log.i("test", "showPower: " + Arrays.toString(getPower.getPower()));
        handleData.initSendData(getPower.getPower());
    }

    @OnClick(R.id.buttonScan)
    void btnScan() {
        handleData.setbStartHRTest(!handleData.isbStartHRTest());
        if (handleData.isbStartHRTest()) {
            mButtonScan.setText(R.string.stop);
            Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
            startActivityForResult(newIntent, HandleData.REQUEST_SELECT_DEVICE);
        } else {
            // Disconnect button pressed
            mButtonScan.setText(R.string.start);
            connectStatus.setText("");
            handleData.clearConfig();
        }
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
            deviceName.setText(handleData.getmDevice().getName());
            connectStatus.setText(R.string.connected);
            handleData.saveConfig();
        }

        // **********当设备断开连接的时候***********//
        if (action.equals(UARTService.ACTION_GATT_DISCONNECTED)) {
            connectStatus.setText(R.string.disconnect);
            handleData.getmUARTService().close();
            if (handleData.isConfigValid()) {
                connectStatus.setText(R.string.connecting);
                handleData.reconnect();
            }
        }

        // **********当扫描到设备的时候***********//
        if (action.equals(UARTService.ACTION_GATT_SERVICES_DISCOVERED)) {
            handleData.getmUARTService().enableTXNotification();
            handleData.sleep(400);
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
            handleData.getmUARTService().disconnect();
            handleData.sleep(100);
            handleData.getmUARTService().close();
            if (handleData.isConfigValid()) {
                connectStatus.setText(R.string.connecting);
                handleData.reconnect();
            }
        }
    }
}
