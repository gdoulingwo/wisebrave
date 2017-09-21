package link_work.wisebrave;

import android.util.Log;


public abstract class BaseBleMessage {
//	2.1、一个完整的数据包格式如下（一对“xx”为一个字节的hex码）:
//		 68 	xx		 xxxx		  xx…		xx	    16
//		包头	      功能码	数据长度	         数据域           检验码      尾帧

    public static final String BASE_TAG = "BLE_COM";
    private byte msg_head = 0x68;
    private byte msg_cmd = 0x00;
    private int msg_data_len = 0;
    private byte[] msg_data;

    //private byte msg_check_val = 0;
    private byte msg_tail = 0x16;

    //	public abstract void backToDevices();
    public static String byteArrHexToString(byte[] b) {
        String ret = "";

        for (byte aB : b) {
            String hex = Integer.toHexString(aB & 0xff);
            if (hex.length() % 2 == 1) {
                hex = '0' + hex;
            }
            ret += hex;
        }

        return ret.toUpperCase();
    }

    public byte calCS(byte[] value, int len) {
        int i;
        int cs = 0;

        for (i = 0; i < len; i++) {
            cs = cs + value[i];
        }

        return (byte) (cs & 0xff);
    }

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
        // 如果数据域不为空且长度不为0，就开始获取数据域的内容
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

    public byte[] setMessageByteData(byte cmd, byte[] data, int len) {
        msg_cmd = cmd;

        //if (data != null)
        {
            msg_data_len = len;
            msg_data = data;
        }

        return getSendByteArray();
    }
}

