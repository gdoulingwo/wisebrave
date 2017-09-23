package link_work.wisebrave.BleMsg;

import android.support.annotation.NonNull;
import android.util.Log;


/**
 * 底层数据包处理工具
 */
public abstract class BaseBleMessage {
//	2.1、一个完整的数据包格式如下（一对“xx”为一个字节的hex码）:
//		 68 	xx		 xxxx		  xx…		xx	    16
//		包头	   功能码	数据长度	     数据域    检验码    尾帧

    public static final String BASE_TAG = "BLE_COM";
    // 数据包头
    private byte msg_head = 0x68;
    // 控制码
    private byte msg_cmd = 0x00;
    // 底层数据域的长度
    private int msg_data_len = 0;
    // 底层数据域
    private byte[] msg_data;
    // 数据包尾
    private byte msg_tail = 0x16;

    /**
     * 解析底层数据包
     *
     * @param b 底层数据包
     * @return 返回解析完成后的String
     */
    @NonNull
    public static String byteArrHexToString(byte[] b) {
        String ret = "";

        for (byte aB : b) {
            String hex = Integer.toHexString(aB & 0xff);
            // 最后长度是奇数的话，需要补0处理。
            if (hex.length() % 2 == 1) {
                hex = '0' + hex;
            }
            ret += hex;
        }
        // Log.i("test", "byteArrHexToString: " + ret.toUpperCase());
        return ret.toUpperCase();
    }

    /**
     * 校验码校验
     *
     * @param value 数据域
     * @param len   数据域的长度
     * @return 返回校验码
     */
    public byte calCS(byte[] value, int len) {
        int cs = 0;

        for (int i = 0; i < len; i++) {
            cs = cs + value[i];
        }

        return (byte) (cs & 0xff);
    }

    /**
     * 生成底层数据包
     *
     * @return 返回生成的底层数据包
     */
    public byte[] getSendByteArray() {
        int i;
        byte[] send = new byte[6 + msg_data_len];

        // 包头
        send[0] = msg_head;
        // 控制码
        send[1] = msg_cmd;
        // 数据长度
        send[2] = (byte) (msg_data_len & 0xff);
        // 数据域
        send[3] = (byte) ((msg_data_len >> 8) & 0xff);
        // 如果数据域不为空且长度不为0，就开始设置数据域的内容
        if ((msg_data_len != 0) && (msg_data != null)) {
            for (i = 0; i < msg_data_len; i++) {
                send[4 + i] = msg_data[i];
            }
        }
        // 校验码
        send[4 + msg_data_len] = calCS(send, (4 + msg_data_len));
        // 包尾
        send[5 + msg_data_len] = msg_tail;

        Log.d(BASE_TAG, "send: " + byteArrHexToString(send));

        return send;
    }

    /**
     * 设置底层数据包的属性
     *
     * @param cmd  控制码
     * @param data 数据域数据
     * @param len  数据域的长度
     * @return 放回底层数据包
     */
    byte[] setMessageByteData(byte cmd, byte[] data, int len) {
        // 控制码
        msg_cmd = cmd;

        if (data != null) {
            msg_data_len = len;
            msg_data = data;
        }

        return getSendByteArray();
    }
}

