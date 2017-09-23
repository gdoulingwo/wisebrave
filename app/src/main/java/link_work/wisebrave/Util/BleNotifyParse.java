package link_work.wisebrave.Util;

import android.util.Log;

import link_work.wisebrave.Activity.MainActivity;
import link_work.wisebrave.BleMsg.BaseBleMessage;
import link_work.wisebrave.BleMsg.BleCmd03_getPower;
import link_work.wisebrave.BleMsg.BleCmd05_RemindOnOff;
import link_work.wisebrave.BleMsg.BleCmd06_getData;
import link_work.wisebrave.BleMsg.BleCmd20_syncTime;

/*
* 通知
* */
public class BleNotifyParse extends BaseBleMessage {

    private static final int BUFFER_MAX_LEN = 1024;
    private static BleNotifyParse mBleNotifyParse;
    private byte[] buffer = new byte[BUFFER_MAX_LEN];
    private byte[] bufferTmp = new byte[BUFFER_MAX_LEN];
    private int bufferFront = 0;   //队列尾
    private int bufferRear = 0;  //队列头
    private int bufferLen = 0;  //队列头

    /*
    * 单例模式
    * */
    public static BleNotifyParse getInstance() {
        if (mBleNotifyParse == null) {
            mBleNotifyParse = new BleNotifyParse();
        }
        return mBleNotifyParse;
    }

    public void doParse(MainActivity hrDK, byte[] notifyData) {
        //synchronized (mNotifyLock) {
        l_doParse(hrDK, notifyData);
        //}
    }

    /**
     * 数据解析，采用循环队列对长连接数据进行处理
     *
     * @param hrDK       显示界面
     * @param notifyData 底层数据
     */
    private void l_doParse(MainActivity hrDK, byte[] notifyData) {
        // 加入循环队列
        Log.d(BaseBleMessage.BASE_TAG, "notify: " + BaseBleMessage.byteArrHexToString(notifyData));
        // 从底层数据数组中逐个取出来处理
        for (byte aNotifyData : notifyData) {
            if (bufferLen >= BUFFER_MAX_LEN) {
                // 找到队列头
                bufferRear = (bufferRear + 1) % BUFFER_MAX_LEN;
                bufferLen--;
            }
            buffer[bufferFront] = aNotifyData;
            // 队列尾
            bufferFront = (bufferFront + 1) % BUFFER_MAX_LEN;
            bufferLen++;
        }

        // 通知索引
        int notifyIndex = 0;
        int msgLen = 0;
        int pos;
        // 分段解析包的段数
        int step = 0;

        for (int read = 0; read < bufferLen; ) {
            pos = (bufferRear + read) % BUFFER_MAX_LEN;
            read++;
            // 分段拆包
            switch (step) {
                case 0:
                    // 如果是数据头的话，
                    if (buffer[pos] == 0x68) {
                        notifyIndex = 0;
                        // 临时数组
                        bufferTmp[notifyIndex++] = buffer[pos];
                        bufferRear = pos;
                        bufferLen = bufferLen - read + 1;
                        read = 1;
                        step++;
                    }
                    break;
                case 1:
                    //
                    bufferTmp[notifyIndex++] = buffer[pos];
                    if (notifyIndex >= 4) {
                        // 获取数据域的数据长度
                        msgLen = 0xff & bufferTmp[2];
                        msgLen = msgLen + ((0x00ff & bufferTmp[3]) << 8);
                        // 如果数据域大小超出256的话，就直接抛弃
                        if (msgLen > 256) {
                            step = 0;
                            bufferRear = (bufferRear + 1) % BUFFER_MAX_LEN;
                            bufferLen--;
                            read = 0;
                            break;
                        }
                        step++;
                    }
                    break;
                case 2:
                    bufferTmp[notifyIndex++] = buffer[pos];
                    if (notifyIndex >= (msgLen + 6)) {
                        if (buffer[pos] == 0x16) {
                            Comm_Handle(hrDK, bufferTmp, notifyIndex);
                            bufferLen = bufferLen - notifyIndex;
                            bufferRear = (pos + 1) % BUFFER_MAX_LEN;
                            read = 0;
                        } else {
                            bufferRear = (bufferRear + 1) % BUFFER_MAX_LEN;
                            bufferLen--;
                            read = 0;
                        }
                        step = 0;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 控制码识别
     *
     * @param hrDK       Activity
     * @param notifyData 底层数据
     * @param dataLength 底层数据长度
     */
    private boolean Comm_Handle(MainActivity hrDK, byte[] notifyData, int dataLength) {

        //int ret = -1;
        byte[] ret = null;

        byte[] frame = new byte[dataLength];
        System.arraycopy(notifyData, 0, frame, 0, dataLength);

        Log.d(BaseBleMessage.BASE_TAG, "rec: " + BaseBleMessage.byteArrHexToString(frame));
        //byte head = notifyData[0];
        byte cmd = frame[1];
        int dataLen = bytes2Char(frame, 2);

        if (dataLen > 1000 || dataLength < dataLen) {
            return false;
        }

        byte[] Data = new byte[dataLen];
        System.arraycopy(frame, 4, Data, 0, dataLen);
        //notifyData = theData;

        if ((cmd & 0x80) == 0x80) {
            // 手环回复给手机的数据部分
            byte req_cmd = (byte) (cmd & 0x3f);
            if (req_cmd == BleCmd03_getPower.mTheCmd) {
                BleCmd03_getPower ctrlCmd = new BleCmd03_getPower();
                ret = ctrlCmd.dealBleResponse(Data, dataLen);
            } else if (req_cmd == BleCmd05_RemindOnOff.mTheCmd) {
                BleCmd05_RemindOnOff ctrlCmd = new BleCmd05_RemindOnOff();
                ret = ctrlCmd.dealBleResponse(Data, dataLen);
            } else if (req_cmd == BleCmd06_getData.mTheCmd) {
                BleCmd06_getData ctrlCmd = new BleCmd06_getData();
                ret = ctrlCmd.dealBleResponse(Data, dataLen);
            } else if (req_cmd == BleCmd20_syncTime.mTheCmd) {
                BleCmd20_syncTime ctrlCmd = new BleCmd20_syncTime();
                ret = ctrlCmd.dealBleResponse(Data, dataLen);
            } else {
                return false;
            }
        }

        if (ret != null) {
            hrDK.initSendData(ret);
        }

        return true;
    }

    private int bytes2Char(byte[] data, int offset) {
        int va = data[offset] & 0xff;
        int vb = (data[offset + 1] << 8) & 0xffff;
        return va + vb;
    }
}
