package link_work.wisebrave.Service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.greenrobot.eventbus.EventBus;

import link_work.wisebrave.Bean.UARTBean;

public class UARTStatusChangeReceiver extends BroadcastReceiver {

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
