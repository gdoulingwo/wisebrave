package link_work.wisebrave.BleMsg;

import android.util.Log;

import link_work.wisebrave.Activity.MainActivity;


public class BleCmd05_RemindOnOff extends BaseBleMessage {

    public static final String TAG = "tixing";
    /**
     * 蓝牙自动关闭广播
     */
    public static final byte mTheCmd = 0x05;
    /*
    * 防丢提醒
    * */
    public static final int REMIND_TYPE_LOST = 1;
    /*
    * 短信提醒
    * */
    public static final int REMIND_TYPE_SMS = 2;
    /*
    * 来电提醒
    * */
    public static final int REMIND_TYPE_PHONE = 3;

    /**
     * 切换提醒功能
     *
     * @param remindType 提醒的类型
     * @param remindOnOff 是否开启提醒
     * */
    public byte[] switchRemind(int remindType, boolean remindOnOff) {
        byte[] data = new byte[3];
        data[0] = 0x00;
        switch (remindType) {
            case REMIND_TYPE_LOST: {
                // LOST 防丢提醒
                data[1] = 0x01;
                data[2] = (byte) (remindOnOff ? 0x01 : 0x00);
                return setMessageByteData(mTheCmd, data, data.length);
            }
            case REMIND_TYPE_SMS: {
                // sms 短信提醒
                data[1] = 0x02;
                data[2] = (byte) (remindOnOff ? 0x01 : 0x00);
                return setMessageByteData(mTheCmd, data, data.length);
            }
            case REMIND_TYPE_PHONE: {
                // phone 来电提醒
                data[1] = 0x03;
                data[2] = (byte) (remindOnOff ? 0x01 : 0x00);
                return setMessageByteData(mTheCmd, data, data.length);
            }
            default:
                break;
        }

        return null;
    }

    /**
     * 读取提醒功能的功能码
     * @param type 1：防丢提醒 2：短信提醒 3：来电提醒
     * @return 0：关  1：开
     */
    public byte[] readRemindStatus(int type) {
        byte[] data = new byte[2];
        // 新协议
        data[0] = 0x01;
        switch (type) {
            case REMIND_TYPE_LOST:
                data[1] = 0x01;
                break;
            case REMIND_TYPE_SMS:
                data[1] = 0x02;
                break;
            case REMIND_TYPE_PHONE:
                data[1] = 0x03;
                break;
            default:
                return null;
        }

        return setMessageByteData(mTheCmd, data, data.length);
    }

    /**
     * 存入缓存
     *
     * @param notifyData 提醒数据内容
     * @param dataLen 提醒数据内容的长度
     */
    public byte[] dealBleResponse(byte[] notifyData, int dataLen) {
        if (dataLen <= 2) {
            return null;
        }
        // ru: 01 01 01
        int type = notifyData[1];
        boolean isOpened = (notifyData[2] == 0x01);

        switch (type) {
            case REMIND_TYPE_LOST:
                Log.d("BEL_status", "===========防丢状态：" + isOpened);
                MainActivity.hrDK.mToggleButtonLose.setChecked(isOpened);
                break;
            case REMIND_TYPE_SMS:
                Log.d("BEL_status", "===========短信状态：" + isOpened);
                MainActivity.hrDK.mToggleButtonSms.setChecked(isOpened);
                break;
            case REMIND_TYPE_PHONE:
                Log.d("BEL_status", "===========电话状态：" + isOpened);
                MainActivity.hrDK.mToggleButtonCall.setChecked(isOpened);
                break;
            default:
                break;
        }
        return null;
    }

}
