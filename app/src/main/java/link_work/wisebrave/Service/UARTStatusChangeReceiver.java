package link_work.wisebrave.Service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;

import link_work.wisebrave.Activity.MainActivity;
import link_work.wisebrave.Bean.UARTBean;
import link_work.wisebrave.BleMsg.BaseBleMessage;
import link_work.wisebrave.R;
import link_work.wisebrave.Util.BleNotifyParse;
import link_work.wisebrave.Util.Config;

public class UARTStatusChangeReceiver extends BroadcastReceiver {
    public static final String TAG = "nRFUART";
    public UARTStatusChangeReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UARTBean uartBean = new UARTBean();
        String action = intent.getAction();
        if (action.equals(UARTService.ACTION_DATA_AVAILABLE)) {
            uartBean.setData(intent.getByteArrayExtra(UARTService.EXTRA_DATA));
        }
        uartBean.setAction(action);
        EventBus.getDefault().post(uartBean);
    }

}
